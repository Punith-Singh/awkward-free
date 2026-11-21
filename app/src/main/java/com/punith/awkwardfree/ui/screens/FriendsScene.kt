package com.punith.awkwardfree.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.graphics.PathParser
import androidx.core.graphics.withSave
import kotlin.math.*

@Composable
internal fun FriendsScene(position: () -> Float, settledPage: Int, modifier: Modifier = Modifier) {
    val scene = remember { CafeScene() }
    val transition = rememberInfiniteTransition(label = "CafÃƒÂ© gestures")
    val clock by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6400, easing = LinearEasing)), label = "Acting cycle")
    val beats = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(settledPage) {
        beats[settledPage].snapTo(0f)
        beats[settledPage].animateTo(1f, tween(4800, easing = LinearEasing))
    }
    val description = when (settledPage) {
        0 -> "Two friends pass a rupee coin across a cafÃƒÂ© table."
        1 -> "She raises her phone and dictates a tab as her friend listens."
        else -> "She sends a reminder. Her friend checks his phone and nods."
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val progress = position().coerceIn(0f, 2f)
        val voice = smooth(1f - abs(progress - 1f))
        val reminder = smooth(progress - 1f)
        drawIntoCanvas {
            scene.draw(it.nativeCanvas, size.width, size.height, clock, voice, reminder,
                beats[0].value, beats[1].value, beats[2].value)
        }
    }
}

private fun smooth(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun phase(time: Float, start: Float, end: Float) = smooth((time - start) / (end - start))
private fun mix(start: Float, end: Float, amount: Float) = start + (end - start) * amount
private fun blink(time: Float, at: Float): Float =
    phase(time, at, at + .014f) - phase(time, at + .016f, at + .042f)

private class CafeScene {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paths = mutableMapOf<String, Path>()
    private val colors = mutableMapOf<String, Int>()
    private val ink = "#302439"
    private val lavender = "#9276B2"
    private val teal = "#387870"
    private val gold = "#F8CA66"
    private val typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun fill(color: String) {
        paint.style = Paint.Style.FILL
        paint.color = colors.getOrPut(color) { Color.parseColor(color) }
    }

    private fun Canvas.shape(color: String, data: String) {
        fill(color)
        drawPath(paths.getOrPut(data) { requireNotNull(PathParser.createPathFromPathData(data)) }, paint)
    }

    private fun Canvas.oval(color: String, x: Float, y: Float, rx: Float, ry: Float) {
        fill(color)
        drawOval(x-rx, y-ry, x+rx, y+ry, paint)
    }

    private fun Canvas.round(color: String, x: Float, y: Float, w: Float, h: Float, radius: Float) {
        fill(color)
        drawRoundRect(x, y, x+w, y+h, radius, radius, paint)
    }

    private fun Canvas.line(color: String, x: Float, y: Float, u: Float, v: Float, width: Float) {
        fill(color)
        paint.strokeWidth = width
        paint.strokeCap = Paint.Cap.ROUND
        drawLine(x, y, u, v, paint)
    }

    fun draw(canvas: Canvas, width: Float, height: Float, time: Float, voice: Float, reminder: Float, giftBeat: Float, recordingBeat: Float, messageBeat: Float) = with(canvas) {
        drawColor(0xffefddc7.toInt())
        withSave {
            val zoom = max(width/360f, height/222f)
            translate((width-360f*zoom)/2, (height-222f*zoom)/2)
            scale(zoom, zoom)
            // Push in toward the speaker and ease back to both friends for the reply.
            val gift = (1f - voice - reminder).coerceIn(0f, 1f)
            val camera = 1f + .035f * voice + .015f * reminder
            scale(camera, camera, mix(92f, 245f, reminder), 117f)
            scenery()

            // The receiver opens a palm before the giver lets go and withdraws.
            val offer = phase(giftBeat, .12f, .43f) - phase(giftBeat, .60f, .85f)
            val receive = phase(giftBeat, .23f, .43f) - phase(giftBeat, .68f, .94f)
            val transfer = phase(giftBeat, .48f, .56f)
            val speaking = phase(recordingBeat, .28f, .34f) - phase(recordingBeat, .72f, .78f)
            val syllable = speaking * (.35f + .65f * sin(recordingBeat * 45f).pow(2))
            val lift = phase(recordingBeat, .12f, .28f) - .68f * phase(recordingBeat, .82f, .98f)
            val send = phase(messageBeat, .10f, .27f)
            val read = phase(messageBeat, .42f, .64f)
            val nod = sin(2f * Math.PI * phase(messageBeat, .64f, .91f)).toFloat()
            val leftHold = voice * lift + reminder * send
            val rightHold = reminder * read
            val leftDock = 129f
            val rightDock = 232f
            val leftPhoneY = 186f - 59f * voice * lift - 45f * reminder * send
            val rightPhoneY = 187f - 53f * rightHold
            val leftPhoneX = leftDock - 10f * leftHold
            val rightPhoneX = rightDock + 8f * rightHold
            val leftPick = voice * phase(recordingBeat, .02f, .12f) + reminder * phase(messageBeat, .01f, .10f)
            val rightPick = reminder * phase(messageBeat, .31f, .42f)
            val leftX = mix(113f + 65f * offer * gift, leftPhoneX, leftPick)
            val leftY = mix(158f - 15f * offer * gift, leftPhoneY + 1f, leftPick)
            val rightX = mix(242f - 62f * receive * gift, rightPhoneX, rightPick)
            val rightY = mix(160f - 17f * receive * gift, rightPhoneY + 1f, rightPick)
            val leftGaze = gift * (phase(giftBeat, .04f, .12f) - phase(giftBeat, .60f, .77f)) + leftPick
            val rightGaze = gift * (phase(giftBeat, .17f, .26f) - phase(giftBeat, .77f, .93f)) + rightPick
            val breath = sin(2f * Math.PI * time).toFloat()
            withSave {
                translate(85f, 0f)
                friend(true, leftX - 85f, leftY, 6f * offer * gift + 2f * leftHold + .5f * breath,
                    4f * leftGaze - .7f * syllable, leftGaze, blink(time, .19f), voice * syllable)
            }
            withSave {
                translate(275f, 0f)
                scale(-1f, 1f)
                friend(false, 275f - rightX, rightY, 5f * receive * gift + 2f * rightHold - .4f * breath,
                    4f * rightGaze + 2f * nod * reminder, rightGaze, blink(time, .63f), 0f)
            }
            table()
            phone(leftPhoneX, leftPhoneY, leftHold, -12f)
            phone(rightPhoneX, rightPhoneY, rightHold, 10f)
            // The coin remains full size while it changes hands.
            if (gift > 0f) withSave {
                translate(mix(leftX + 1f, rightX - 1f, transfer), mix(leftY, rightY, transfer) - 5f)
                scale(gift, gift)
                oval("#AD6C31",0f,1f,7f,7f)
                oval(gold,0f,-.5f,6.4f,6.4f)
                fill(ink)
                paint.typeface = typeface
                paint.textSize = 9f
                paint.textAlign = Paint.Align.CENTER
                drawText("\u20b9",0f,2.8f,paint)
            }
            withSave {
                translate(179f,64f)
                val reveal = voice * phase(recordingBeat, .25f, .34f)
                scale(reveal,reveal)
                val confirmed = recordingBeat >= .83f
                round("#FFFAF4",-62f,-18f,124f,36f,12f)
                shape("#FFFAF4","M-46,14 L-52,24 L-33,15 Z")
                val words = when {
                    confirmed -> "Lunch   \u20B9500"
                    recordingBeat > .58f -> "\u20b9500 for lunch"
                    recordingBeat > .41f -> "I lent Rahul \u20b9500"
                    recordingBeat > .23f -> "I lent Rahul\u2026"
                    else -> "\u2026"
                }
                fill(ink)
                paint.textSize = 11f
                paint.typeface = typeface
                paint.textAlign = Paint.Align.CENTER
                drawText(words, 5f, 4f, paint)
                if (confirmed) {
                    line(teal,-55f,0f,-52f,3f,2f)
                    line(teal,-52f,3f,-46f,-4f,2f)
                } else {
                    for (i in 0..2) {
                        val bar = 1.5f + syllable * (if(i==1) 6f else 3f)
                        line("#745193",-55f+3*i,-bar,-55f+3*i,bar,1.8f)
                    }
                }
            }
            withSave {
                val travel=smooth((messageBeat-.16f)/.50f)
                translate(136f+59f*travel,75f-10f*sin(travel*Math.PI).toFloat())
                val visible = reminder * phase(messageBeat, .24f, .33f)
                scale(visible, visible)
                round("#FFFAEC",-18f,-12f,36f,24f,8f)
                shape("#FFFAEC","M8,8 L14,17 L14,8 Z")
                line(teal,-6f,0f,-2f,4f,2.4f); line(teal,-2f,4f,6f,-5f,2.4f)
            }
        }
    }

    private fun Canvas.friend(woman: Boolean, handX: Float, handY: Float, tilt: Float, headTilt: Float, gaze: Float, blink: Float, speaking: Float) {
        val skin = if(woman) "#DE9F79" else "#B67653"
        val shadow = if(woman) "#BE7D63" else "#95583F"
        val shirt = if(woman) lavender else teal
        val seam = if(woman) "#785E92" else "#305F60"
        // A shoulder pivot keeps the head, collar and far arm attached during the lean.
        withSave {
            rotate(tilt,0f,161f)
            // The relaxed arm rests on the table, behind the gesturing arm.
            shape(seam,"M16,129 Q35,126 39,148 L46,174 Q48,182 43,186 Q32,191 28,177 L17,151 Z")
            shape(skin,"M39,172 Q43,169 47,174 L54,180 Q59,179 62,182 L65,185 Q66,188 60,188 L44,186 Q38,183 36,177 Z")
            shape(shirt,"M-61,192 Q-59,144 -42,131 Q-31,124 -13,120 L10,120 Q34,123 40,143 L47,192 Z")
            shape(seam,"M-59,192 Q-53,160 -45,151 L-41,176 L-24,183 L-22,192 Z")
            shape("#F8EBD9","M-19,124 L10,122 L21,134 L9,183 Z")
            shape(skin,"M-15,92 L8,96 L10,122 Q6,133 -2,133 L-17,124 Z")
            shape(shadow,"M-15,103 L7,102 L8,113 Q-1,118 -15,113 Z")
            shape(seam,"M-17,119 L4,148 L-19,136 L-25,139 L-30,129 Z")
            shape(shirt,"M-17,118 L1,143 L-17,132 L-24,134 L-28,128 Z")
            shape(seam,"M11,119 L22,126 L14,139 L7,145 Z")
            if (woman) {
                shape("#B19AC9","M-41,134 Q-34,126 -26,125 L-30,131 L-37,139 Z")
                shape("#83679F","M17,149 Q21,152 33,147 L30,162 Q25,169 20,163 Z")
            } else {
                shape("#B1C7AB","M17,144 L34,140 L34,143 L18,148 Z")
                line(seam, 17f,149f,18f,159f,.85f)
                line(seam, 18f,159f,24f,162f,.85f)
                line(seam, 24f,162f,32f,157f,.85f)
                oval("#EAD8B4",7f,150f,1.3f,1.3f)
            }
            withSave {
                rotate(headTilt,0f,112f)
                if (woman) {
                    // A loose wavy bob frames her face, with a small bun at the back.
                    shape("#372B37", "M-34,79 C-43,69 -42,51 -35,41 C-28,30 -15,27 -2,30 C13,28 27,37 29,51 C33,61 30,74 27,82 C31,92 30,108 23,114 C14,120 -18,120 -30,113 C-40,107 -42,96 -34,79 Z")
                    shape("#372B37", "M-29,50 C-44,53 -51,47 -50,37 C-49,28 -41,23 -32,26 C-24,27 -21,34 -23,42 Z")
                    shape("#6E4D51", "M-46,37 C-45,29 -39,27 -34,28 Q-42,31 -43,38 Z")
                    shape("#D9A85D", "M-30,34 Q-25,31 -22,35 L-22,38 Q-26,35 -30,38 Z")
                } else {
                    // A textured crop and a soft sideburn give him his own silhouette.
                    shape("#30272C", "M-33,75 C-39,61 -36,46 -28,37 C-20,28 -5,29 6,31 C20,28 30,38 31,50 C35,59 32,70 27,79 L25,99 Q20,108 13,110 L-24,105 Z")
                    oval("#30272C",-28f,43f,9f,9f)
                    oval("#30272C",-18f,36f,10f,9f)
                    oval("#30272C",-6f,35f,10f,9f)
                    oval("#30272C",6f,35f,10f,9f)
                    oval("#30272C",17f,39f,10f,9f)
                    shape("#5D4547","M-30,42 Q-24,31 -13,33 Q-23,36 -27,44 Z")
                    shape("#62494A","M-11,34 Q-1,29 8,34 Q-2,33 -9,38 Z")
                }
                // Rounded three-quarter head with the nearer cheek and ear visible.
                oval(skin,-25f,79f,7f,9f)
                shape(skin, "M-25,62 C-24,49 -14,42 -2,42 C12,42 23,51 24,65 L24,86 C24,102 15,112 3,113 C-10,113 -20,105 -23,93 Q-26,82 -25,62 Z")
                shape(shadow, "M-27,74 Q-32,75 -31,81 Q-30,86 -26,87 Q-28,83 -27,80 Q-25,78 -23,82 Q-23,76 -27,74 Z")
                if (woman) {
                    shape("#372B37","M-29,70 C-34,57 -30,43 -21,37 C-11,30 5,32 15,38 Q25,45 25,58 Q18,58 11,52 Q3,61 -8,63 Q-15,65 -21,64 L-23,77 Z")
                    shape("#674A50","M-26,53 Q-17,36 -3,37 Q-16,41 -24,56 Z")
                    shape("#76535A","M-13,37 Q0,32 12,40 Q2,37 -8,42 Z")
                    shape("#372B37","M-23,62 Q-17,70 -19,81 Q-20,88 -24,93 Q-22,80 -27,74 Z")
                    oval("#E4B86F",-26f,91f,3f,4f)
                    oval("#A16B58",-26f,91f,1.7f,2.7f)
                } else {
                    shape("#30272C","M-28,71 Q-32,56 -25,44 Q-18,35 -4,37 Q9,33 20,42 Q26,47 25,58 Q18,58 12,53 Q4,59 -5,57 Q-13,64 -21,64 L-22,77 Z")
                    shape("#30272C","M-24,65 Q-20,69 -21,79 L-23,86 Q-25,82 -25,77 Z")
                    shape("#9E674F","M-19,100 Q-12,109 1,111 Q13,112 21,102 Q17,113 4,115 Q-12,113 -19,103 Z")
                }
                // Soft, expressive features remain attached to the head as it turns.
                val eyeOpen = 1f - blink
                val eyeShift = .7f - .5f * gaze
                val brow = if (woman) "#5A3940" else "#493139"
                shape(brow, "M-16,70 Q-11,66.8 -5.5,69 Q-10.5,68.3 -15.5,71 Z")
                shape(brow, "M5.5,69 Q11,66.8 16,70 L15.5,71 Q10.5,68.3 5.5,70 Z")
                // Almond-shaped eyes, warm irises and tiny highlights avoid a startled look.
                shape(if (woman) "#F9E5D7" else "#EAD1BD", "M-15.2,76 Q-10.8,72.3 -6,75.8 Q-10.5,79.3 -15.2,76 Z")
                shape(if (woman) "#F9E5D7" else "#EAD1BD", "M6,75.8 Q10.8,72.3 15.2,76 Q10.5,79.3 6,75.8 Z")
                oval("#543A35",-10.5f+eyeShift,76f,1.75f,1.8f*eyeOpen+.12f)
                oval("#543A35",10.5f+eyeShift,76f,1.75f,1.8f*eyeOpen+.12f)
                if (eyeOpen > .5f) {
                    oval("#FFF8EC",-10.9f+eyeShift,75.4f,.52f,.52f)
                    oval("#FFF8EC",10.1f+eyeShift,75.4f,.52f,.52f)
                } else {
                    line(brow,-15f,76f,-6f,76f,1.15f)
                    line(brow,6f,76f,15f,76f,1.15f)
                }
                // Gentle cheek colour, a small nose and an asymmetrical smile.
                oval(if (woman) "#D89080" else "#A86550",-15f,87f,4.4f,2.2f)
                oval(if (woman) "#D89080" else "#A86550",15f,87f,4.4f,2.2f)
                shape(if (woman) "#BF8069" else "#915942",
                    "M1,82 Q-1,87 1.5,89 Q3.5,90 6,88.5 Q3,89 2.5,87 Q2.2,85 3,82 Z")
                shape(if (woman) "#965B58" else "#74473F",
                    "M-4,96 Q2,100.5 10,96 Q7,101.8 2.2,101 Q-1.5,100.5 -4,96 Z")
                line(if (woman) "#B8776D" else "#985E4E",-1f,103f,5f,103.5f,.65f)
                if (speaking > .1f) oval("#784A49",3.5f,99f,2.7f,1.2f*speaking)

            }
        }
        // A two-bone arm bends at the elbow as the wrist moves between targets.
        val shoulderX=-17f + sin(tilt * Math.PI.toFloat() / 180f) * 18f
        val shoulderY=143f
        val dx=handX-shoulderX
        val dy=handY-shoulderY
        val length=hypot(dx,dy)
        val bend=sqrt((57f*57f-length*length/4f).coerceAtLeast(0f))
        val elbowX=(shoulderX+handX)/2-dy/length*bend
        val elbowY=(shoulderY+handY)/2+dx/length*bend
        armSegment(shoulderX, shoulderY, elbowX, elbowY, shirt,
            "M-2,-11 C10,-13 31,-10 49,-8 Q57,-2 50,8 C32,11 10,13 -2,11 Q-13,1 -2,-11 Z")
        armSegment(elbowX, elbowY, handX, handY, skin,
            "M0,-7 Q14,-8 30,-5 L49,-3 L51,3 Q37,3 22,7 Q6,10 0,7 Q-7,2 0,-7 Z")
        armSegment(elbowX, elbowY, handX, handY, seam,
            "M-1,-8 Q4,-10 16,-8 L18,6 Q10,10 1,9 Q-9,6 -7,-1 Q-6,-6 -1,-8 Z")
        withSave {
            translate(handX, handY)
            rotate(-10f - 8f * gaze)
            shape(skin, "M-6,-3 Q-3,-5 0,-5 L3,-9 Q4,-10 5,-9 Q6,-8 5,-6 L3,-3 L9,-4 Q11,-4 11,-2.7 Q11,-1.7 9.5,-1.4 L11,-1.4 Q12,-1 11.5,0 Q11,1 10,1 L9.5,1 Q10.5,1.5 10,2.5 Q9.7,3.2 8,3.4 L6.5,3.5 Q7.2,4 6.5,4.8 Q5.8,5.3 4,5.3 L-1,5 Q-4,5 -6,2 Z")
            line(shadow, 6f,-1.3f,9.5f,-1.4f,.55f)
            line(shadow, 6.5f,1f,9.5f,1f,.55f)
            line(shadow, 5.5f,3.4f,8f,3.4f,.5f)
        }
    }

    private fun Canvas.armSegment(x: Float, y: Float, endX: Float, endY: Float, color: String, contour: String) = withSave {
        translate(x, y)
        rotate(Math.toDegrees(atan2(endY-y, endX-x).toDouble()).toFloat())
        scale(hypot(endX-x, endY-y) / 50f, 1f)
        shape(color, contour)
    }

    private fun Canvas.phone(x: Float,y: Float,lift: Float,angle: Float) = withSave {
        translate(x, y - 3f * lift)
        rotate(mix(0f, angle, lift))
        scale(1f, mix(.30f, 1f, lift))
        round(ink,-7f,-11f,14f,25f,3f)
        round("#CEBADC",-5f,-8f,10f,17f,1.2f)
        line("#FBF2E5",-2f,11f,2f,11f,.9f)
    }

    private fun Canvas.scenery() {
        // Sunlit shopfront across the street, seen through the cafe window.
        round("#F8E9CD",-100f,-90f,560f,223f,0f)
        oval("#F0C27B",205f,42f,20f,20f)
        shape("#DAC8AE","M-40,107 L-40,74 L8,69 L8,78 L42,78 L42,62 L78,62 L78,98 L119,98 L119,82 L147,82 L147,97 L192,97 L192,73 L224,73 L224,65 L264,65 L264,95 L307,95 L307,75 L401,75 L401,132 L-40,132 Z")
        for (x in 0..5) {
            round("#BEAC96", 119f + x * 5f, 88f, 2.4f, 4f, .5f)
            round("#BEAC96", 226f + x * 6f, 73f, 3f, 7f, .6f)
        }
        shape("#DEC9AB","M-50,-10 L38,-10 L-15,122 L-70,122 Z")
        shape("#FFF3DB","M143,-10 L156,-10 L101,117 L88,117 Z")
        for (x in listOf(34f, 176f, 328f)) {
            round("#C7A88B",x,0f,3.5f,124f,0f)
            round("#F8E8CB",x+3f,0f,1f,124f,0f)
        }
        round("#C09B81",-50f,118f,460f,6f,0f)
        round("#85685F",-50f,133f,460f,65f,11f)
        round("#AC7A65",-50f,134f,460f,53f,8f)
        line("#C59B7D",-50f,138f,410f,138f,1.7f)
        for (x in 0..12) line("#956D5F",x*32f,141f,x*32f,188f,1f)
        // Canvas awning, with a scalloped hem and a warm shadow.
        round("#DFC7AA",-60f,-6f,480f,23f,2f)
        for (i in -5..13) withSave {
            translate(i * 30f, 0f)
            shape(if (i % 2 == 0) "#C37F63" else "#F3DDBB",
                "M5,-8 L35,-8 L30,10 Q28,16 24,16 L5,16 Q1,16 0,12 L0,10 Z")
        }
    }

    private fun Canvas.table() {
        shape("#D7AB87","M-100,186 Q156,178 460,186 L460,250 L-100,250 Z")
        shape("#B88469","M-100,205 Q180,199 460,204 L460,250 L-100,250 Z")
        shape("#EAC49E","M-70,203 Q180,198 410,202 Q200,201 -70,205 Z")
        oval("#BB9279",181f,191f,17f,2.4f)
        withSave {
            translate(181f,188f)
            rotate(-7f)
            shape("#FAEBD1","M-12,-4 L11,-4 L14,4 L-10,4 Z")
            line("#BFAB96",-7f,-1f,6f,-1f,.8f)
            line("#BFAB96",-6f,1f,2f,1f,.8f)
        }
        for (x in listOf(44f,296f)) withSave {
            translate(x,190f)
            oval("#A87664",0f,4f,18f,3f)
            oval("#F4DBB8",12f,-6f,6f,6f); oval("#BA8B78",13f,-6f,3f,3f)
            shape("#F9E6C7","M-12,-15 L12,-15 L10,-1 Q8,5 -2,4 Q-10,4 -11,-2 Z")
            oval("#DBBD99",0f,-15f,12f,3.5f); oval("#785046",0f,-15f,10f,2f)
        }
    }
}
