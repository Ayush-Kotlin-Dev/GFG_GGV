package com.ayush.geeksforgeeks.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Create a function to handle vibration
object VibratorService {
    // Predefined vibration patterns
    sealed class VibrationPattern(val timing: Long, val amplitude: Int) {
        object Tick : VibrationPattern(20, 50)  // Quick tick
        object Click : VibrationPattern(50, 100)  // Standard click
        object HeavyClick : VibrationPattern(100, 255)  // Strong click
        object DoubleClick : VibrationPattern(50, 150)  // Double vibration
        object Error : VibrationPattern(150, 200)  // Error feedback
        object Success : VibrationPattern(75, 150)  // Success feedback

        // Custom pattern with specific timing and amplitude
        class Custom(timing: Long, amplitude: Int) : VibrationPattern(timing, amplitude)
    }

    // Predefined vibration sequences
    sealed class VibrationSequence(val timings: LongArray, val amplitudes: IntArray) {
        object DoubleTap : VibrationSequence(
            longArrayOf(0, 50, 50, 50),
            intArrayOf(0, 100, 0, 100)
        )
        object SuccessPattern : VibrationSequence(
            longArrayOf(0, 50, 50, 100),
            intArrayOf(0, 150, 0, 200)
        )
        object ErrorPattern : VibrationSequence(
            longArrayOf(0, 100, 50, 100),
            intArrayOf(0, 200, 0, 200)
        )
        object NotificationPattern : VibrationSequence(
            longArrayOf(0, 50, 100, 50),
            intArrayOf(0, 100, 0, 150)
        )

        // Custom sequence with specific timings and amplitudes
        class Custom(timings: LongArray, amplitudes: IntArray) : VibrationSequence(timings, amplitudes)
    }

    @SuppressLint("ServiceCast")
    fun vibrate(
        context: Context,
        pattern: VibrationPattern,
        delay: Long = 0,
        repeat: Int = 1,
        repeatDelay: Long = 0
    ) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Handle multiple vibrations if repeat > 1
                if (repeat > 1) {
                    val timings = mutableListOf<Long>()
                    val amplitudes = mutableListOf<Int>()

                    // Add initial delay if specified
                    if (delay > 0) {
                        timings.add(delay)
                        amplitudes.add(0)
                    }

                    // Create pattern for repeated vibrations
                    repeat(repeat) {
                        timings.add(pattern.timing)
                        amplitudes.add(pattern.amplitude)
                        if (it < repeat - 1 && repeatDelay > 0) {
                            timings.add(repeatDelay)
                            amplitudes.add(0)
                        }
                    }

                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            timings.toLongArray(),
                            amplitudes.toIntArray(),
                            -1
                        )
                    )
                } else {
                    // Single vibration with delay
                    if (delay > 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(delay)
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    pattern.timing,
                                    pattern.amplitude
                                )
                            )
                        }
                    } else {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                pattern.timing,
                                pattern.amplitude
                            )
                        )
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern.timing)
            }
        } catch (e: Exception) {
            Log.e("Vibration", "Error vibrating device", e)
        }
    }

    @SuppressLint("ServiceCast")
    fun vibrateSequence(
        context: Context,
        sequence: VibrationSequence,
        delay: Long = 0
    ) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (delay > 0) {
                    val timingsWithDelay = longArrayOf(delay) + sequence.timings
                    val amplitudesWithDelay = intArrayOf(0) + sequence.amplitudes
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            timingsWithDelay,
                            amplitudesWithDelay,
                            -1
                        )
                    )
                } else {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            sequence.timings,
                            sequence.amplitudes,
                            -1
                        )
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(sequence.timings, -1)
            }
        } catch (e: Exception) {
            Log.e("Vibration", "Error vibrating device", e)
        }
    }
}