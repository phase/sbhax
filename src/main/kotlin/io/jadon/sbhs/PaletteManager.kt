package io.jadon.sbhs

import java.awt.*
import java.awt.event.*
import java.awt.image.*
import java.io.*
import java.util.*
import javax.imageio.*
import javax.swing.*

object PaletteManager {
    val PALETTE_INFO: HashMap<String, Array<String>> = object : HashMap<String, Array<String>>() {
        init {
            val g = arrayOf("Background (useless)", "Eye color and shoe/glove reflection", "Above the eye, shoe/glove color", "Outside of shoe/glove", "Outline of shoe/glove", "In-ball shine", "Primary fur color", "Secondary fur color", "Third fur color/outline", "Primary skin color", "Secondary skin color", "Third skin color", "Primary shoe color", "Secondary shoe color", "Third shoe color", "Eye Color")
            put("Sonic", g)
            put("Knuckles", g)
            put("Tails",
                    arrayOf("Background (useless)", "Eye color and shoe/glove reflection", "Above the eye, tail/hand color", "Primary outline", "Secondary outline", "In-ball shine", "Primary fur color", "Secondary fur color", "Third fur color/outline", "Primary skin color", "Secondary skin color", "Third skin color", "Primary shoe color & Weapons", "Secondary shoe color & Weapons", "Third shoe color & Weapons", "Eye Color"))
            put("Amy", arrayOf("Background (useless)", "Eye color and shoe/hammer reflection", "Above the eye, shoe/glove color"))
        }
    }

    /**
     * "sonic" -> ["23452345", "23452345", ...]
     */
    var PALETTES = HashMap<String, Array<String>>()
    var PALETTE_OFFSET = HashMap<String, Int>()

    fun addPaletteTab(pane: JTabbedPane, name: String, offset: Int) {
        pane.addTab(name, null, createPalettePanel(name, offset), "Edit $name Palette")
    }

    fun createPalettePanel(name: String, offset: Int): JPanel {
        PALETTE_OFFSET.put(name, offset)
        val colors = arrayOfNulls<String>(16)
        var color = 0
        var f = ""
        for (i in offset..offset + 32) {
            SBHS.raf.seek(i.toLong())
            val value = SBHS.raf.read()
            //System.out.println(value + " " + f);
            // System.out.print(Integer.toHexString(value));
            if (f.length == 4) {
                val colorParts = listOf("${f[0]}${f[1]}", "${f[2]}${f[3]}")
                f = colorParts[1] + colorParts[0]
                colors[color++] = f
                //System.out.println("@"+i + " " + color + " "+f);
                f = SBHS.hex(value)
            } else {
                f += SBHS.hex(value)
            }
        }
        PALETTES.put(name, colors.map { it.orEmpty() }.toTypedArray())
        //SpriteManager.printPalette(PALETTES.get(name));
        if (name == "Sonic") {
            val shadowColors = Arrays.asList<String>(*colors).toTypedArray()
            shadowColors[0] = GBAColor.toGBA(Color.white)
            shadowColors[1] = GBAColor.toGBA(Color.gray)
            PALETTES.put("Ground Shadow", shadowColors)
        }
        // System.out.println(Arrays.toString(colors));
        var i = 0
        val jp = JPanel()
        jp.layout = BoxLayout(jp, BoxLayout.Y_AXIS)
        for (s in colors) {
            i++
            val jb = JButton("$name Color $i: " + if (PALETTE_INFO[name] == null)
                "Something?"
            else if (PALETTE_INFO[name]!!.size < i) "Something?" else PALETTE_INFO[name]!![i - 1])
            jb.addActionListener { e: ActionEvent ->
                val title = jb.text
                val colorIndex = -1 + Integer.parseInt(title.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[2].replace(":", ""))
                val gc = GBAColor.toGBA(SBHS.getColorInput(GBAColor.fromGBA(colors[colorIndex].orEmpty())))
                colors[colorIndex] = gc
                PALETTES.remove(name)
                PALETTES.put(name, colors.map { it.orEmpty() }.toTypedArray())
                try {
                    val colorParts = listOf("${gc[0]}${gc[1]}", "${gc[2]}${gc[3]}")
                    val h1 = Integer.parseInt(colorParts[0], 16)
                    val h2 = Integer.parseInt(colorParts[1], 16)
                    // colors[colorIndex] = Integer.toHexString(h2) +
                    // Integer.toHexString(h1) + "";
                    val o = offset + colorIndex * 2.toLong()
                    SBHS.raf.seek(o)
                    SBHS.raf.write(h2)
                    SBHS.raf.seek(o + 1)
                    SBHS.raf.write(h1)
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }

                try {
                    Character.valueOf(name).updateSpriteTab()
                } catch (e: IllegalArgumentException) {
                    // Ignoring because it's probably a PaletteData enum constant
                }
            }
            jp.add(jb)
        }
        val upload = JButton("  Upload Palette")
        upload.addActionListener {
            println("Uploading palette $name")
            val fc = JFileChooser()
            if (fc.showOpenDialog(SBHS.frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    val img = ImageIO.read(fc.selectedFile)
                    for (x in 0..15) {
                        val c = Color(img.getRGB(x, 0))
                        val gc = GBAColor.toGBA(c)
                        println("Upload-color: $gc")
                        colors[x] = gc
                        try {
                            val colorParts = listOf("${gc[0]}${gc[1]}", "${gc[2]}${gc[3]}")
                            val h1 = Integer.parseInt(colorParts[0], 16)
                            val h2 = Integer.parseInt(colorParts[1], 16)
                            val o = offset + x * 2.toLong()
                            SBHS.raf.seek(o)
                            SBHS.raf.write(h2)
                            SBHS.raf.seek(o + 1)
                            SBHS.raf.write(h1)
                        } catch (e1: IOException) {
                            e1.printStackTrace()
                        }

                    }
                    SpriteManager.printPalette(PALETTES[name]!!)
                    PALETTES.remove(name)
                    PALETTES.put(name, colors.map { it.orEmpty() }.toTypedArray())
                    SpriteManager.printPalette(PALETTES[name]!!)
                    Character.valueOf(name).updateSpriteTab()
                } catch (x: IOException) {
                    x.printStackTrace()
                }

            }
        }
        jp.add(upload)
        val save = JButton("  Save Palette")
        save.addActionListener {
            println("Saving palette $name")
            //Create Image
            val img = BufferedImage(16, 1, BufferedImage.TYPE_INT_RGB)
            for (x in 0..15) {
                val c = GBAColor.fromGBA(PALETTES[name]!![x])
                img.setRGB(x, 0, c.rgb)
            }
            //Save Image
            val fc = JFileChooser()
            if (fc.showSaveDialog(SBHS.frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    val o = fc.selectedFile
                    ImageIO.write(img, "png", o)
                } catch (x: IOException) {
                    x.printStackTrace()
                }

            }
        }
        jp.add(save)
        return jp
    }
}
