package com.fluxplayer.player.frc

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.GlObjectsProvider
import androidx.media3.common.GlTextureInfo
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import java.util.concurrent.Executor

/**
 * Baseline Frame Rate Conversion (FRC) OpenGL effect for Media3.
 *
 * Implements [GlEffect] and [GlShaderProgram] to insert an OpenGL ES 2.0/3.0
 * frame interpolation pipeline into ExoPlayer before video frames hit the screen.
 *
 * In this baseline pass, it applies motion-smoothed Frame Blending (mixing Frame N
 * and Frame N-1 with an alpha mask) on the GPU without CPU readbacks.
 */
@OptIn(UnstableApi::class)
class FrcGlEffect(
    @Volatile var blendWeight: Float = 0.5f,
    @Volatile var enabled: Boolean = true
) : GlEffect, GlShaderProgram {

    companion object {
        const val TAG = "FluxFRC"
    }

    private var activeShaderProgram: FrcGlShaderProgram? = null

    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        Log.i(TAG, "Initializing FrcGlShaderProgram: useHdr=$useHdr, blendWeight=$blendWeight, enabled=$enabled")
        val shaderProgram = FrcGlShaderProgram(context, useHdr, blendWeight, enabled)
        activeShaderProgram = shaderProgram
        return shaderProgram
    }

    // --- GlShaderProgram interface delegation ---

    override fun setInputListener(listener: GlShaderProgram.InputListener) {
        activeShaderProgram?.setInputListener(listener)
    }

    override fun setOutputListener(listener: GlShaderProgram.OutputListener) {
        activeShaderProgram?.setOutputListener(listener)
    }

    override fun setErrorListener(executor: Executor, listener: GlShaderProgram.ErrorListener) {
        activeShaderProgram?.setErrorListener(executor, listener)
    }

    override fun queueInputFrame(
        glObjectsProvider: GlObjectsProvider,
        inputTexture: GlTextureInfo,
        presentationTimeUs: Long
    ) {
        activeShaderProgram?.queueInputFrame(glObjectsProvider, inputTexture, presentationTimeUs)
    }

    override fun releaseOutputFrame(outputTexture: GlTextureInfo) {
        activeShaderProgram?.releaseOutputFrame(outputTexture)
    }

    override fun signalEndOfCurrentInputStream() {
        activeShaderProgram?.signalEndOfCurrentInputStream()
    }

    override fun flush() {
        activeShaderProgram?.flush()
    }

    override fun release() {
        activeShaderProgram?.release()
        activeShaderProgram = null
    }
}

/**
 * Shader program extending [BaseGlShaderProgram] to manage texture pools,
 * rendering quads, and frame blending for FRC.
 */
@OptIn(UnstableApi::class)
class FrcGlShaderProgram(
    private val context: Context,
    private val useHdr: Boolean,
    private val blendWeight: Float = 0.5f,
    private val enabled: Boolean = true
) : BaseGlShaderProgram(useHdr, /* outputPoolSize = */ 1) {

    private val glProgram: GlProgram
    private var prevTexId: Int = -1
    private var hasPrevFrame = false
    private var frameCount = 0L
    private var lastFpsLogTime = 0L
    private var framesSinceLastLog = 0
    private var currentWidth = 0
    private var currentHeight = 0

    companion object {
        private const val TAG = "FluxFRC"

        // OpenGL ES 2.0 Vertex Shader
        const val VERTEX_SHADER = """
            attribute vec4 aFramePosition;
            uniform mat4 uTransformationMatrix;
            uniform mat4 uTexTransformationMatrix;
            varying vec2 vTexSamplingCoord;

            void main() {
                gl_Position = uTransformationMatrix * aFramePosition;
                vec4 texturePosition = vec4(aFramePosition.x * 0.5 + 0.5,
                                            aFramePosition.y * 0.5 + 0.5, 0.0, 1.0);
                vTexSamplingCoord = (uTexTransformationMatrix * texturePosition).xy;
            }
        """

        // OpenGL ES 2.0 Fragment Shader (FRC Frame Blending)
        const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexSampler;
            uniform sampler2D uPrevTexSampler;
            uniform float uBlendWeight;
            uniform int uHasPrevFrame;
            uniform int uEnabled;
            varying vec2 vTexSamplingCoord;

            void main() {
                vec4 currentFrame = texture2D(uTexSampler, vTexSamplingCoord);
                if (uEnabled == 1 && uHasPrevFrame == 1) {
                    vec4 prevFrame = texture2D(uPrevTexSampler, vTexSamplingCoord);
                    // Linear frame blend interpolation for high-framerate motion smoothing
                    gl_FragColor = mix(prevFrame, currentFrame, uBlendWeight);
                } else {
                    gl_FragColor = currentFrame;
                }
            }
        """
    }

    init {
        try {
            glProgram = GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            val bounds = GlUtil.getNormalizedCoordinateBounds()
            glProgram.setBufferAttribute("aFramePosition", bounds, 4)
            val identity = GlUtil.create4x4IdentityMatrix()
            glProgram.setFloatsUniform("uTransformationMatrix", identity)
            glProgram.setFloatsUniform("uTexTransformationMatrix", identity)
            glProgram.setFloatUniform("uBlendWeight", blendWeight)
            glProgram.setIntUniform("uEnabled", if (enabled) 1 else 0)
            Log.i(TAG, "FrcGlShaderProgram compiled successfully (blendWeight=$blendWeight, enabled=$enabled)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile FRC GLSL shader program", e)
            throw VideoFrameProcessingException.from(e)
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        if (inputWidth != currentWidth || inputHeight != currentHeight) {
            currentWidth = inputWidth
            currentHeight = inputHeight
            try {
                if (prevTexId != -1) {
                    GlUtil.deleteTexture(prevTexId)
                }
                prevTexId = GlUtil.createTexture(inputWidth, inputHeight, useHdr)
                hasPrevFrame = false
                Log.i(TAG, "FRC configured viewport: ${inputWidth}x$inputHeight, allocated prevTexId=$prevTexId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to allocate previous frame texture for FRC", e)
            }
        }
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        frameCount++
        framesSinceLastLog++
        val now = System.currentTimeMillis()
        if (now - lastFpsLogTime >= 1000L) {
            val fps = (framesSinceLastLog * 1000.0) / (now - lastFpsLogTime).coerceAtLeast(1L)
            val fpsStr = String.format(java.util.Locale.US, "%.1f fps", fps)
            Log.d(TAG, "FRC Active: #$frameCount frames processed | current rate: $fpsStr | pts: ${presentationTimeUs / 1000}ms")
            lastFpsLogTime = now
            framesSinceLastLog = 0
        }

        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, /* texUnitIndex = */ 0)

            if (hasPrevFrame && prevTexId != -1) {
                glProgram.setSamplerTexIdUniform("uPrevTexSampler", prevTexId, /* texUnitIndex = */ 1)
                glProgram.setIntUniform("uHasPrevFrame", 1)
            } else {
                glProgram.setSamplerTexIdUniform("uPrevTexSampler", inputTexId, /* texUnitIndex = */ 1)
                glProgram.setIntUniform("uHasPrevFrame", 0)
            }

            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()

            // Cache rendered frame to prevTexId using fast GPU-side framebuffer copy
            if (prevTexId != -1 && currentWidth > 0 && currentHeight > 0) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, prevTexId)
                GLES20.glCopyTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, currentWidth, currentHeight)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
                hasPrevFrame = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during FRC drawFrame at pts=${presentationTimeUs}us", e)
            throw VideoFrameProcessingException.from(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
            if (prevTexId != -1) {
                GlUtil.deleteTexture(prevTexId)
                prevTexId = -1
            }
            Log.i(TAG, "FRC GL program released")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing FRC GL resources", e)
        }
    }
}

