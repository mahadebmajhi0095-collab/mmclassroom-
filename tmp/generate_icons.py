import subprocess
import os

def run_cmd(cmd):
    print("Running:", cmd)
    res = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if res.returncode != 0:
        print("ERROR:", res.stderr.decode())
    return res.returncode == 0

def main():
    # Colors
    NAVY_BLUE = "#0C132B"
    GOLD = "#D4AF37"
    BEIGE = "#FDFBF7"
    
    # 1. Create base circles canvas (512x512)
    # Background circle (navy blue), gold border, inner divider gold border, and inner beige canvas
    base_cmd = (
        f'convert -size 512x512 xc:transparent '
        f'-fill "{NAVY_BLUE}" -draw "circle 256,256 256,16" ' # Radius 240
        f'-fill none -stroke "{GOLD}" -strokewidth 8 -draw "circle 256,256 256,20" ' # Outer border
        f'-fill "{BEIGE}" -stroke "{GOLD}" -strokewidth 4 -draw "circle 256,256 256,76" ' # Inner beige circle (Radius 180)
        f'/tmp/base_canvas.png'
    )
    run_cmd(base_cmd)
    
    # 2. Create Top Text: "MAHADEB" curved along top arc (100-degree bend)
    top_text_cmd = (
        f'convert -size 400x80 xc:transparent '
        f'-fill "{GOLD}" -font Nimbus-Roman-Bold -pointsize 42 -gravity center -annotate +0+5 "MAHADEB" '
        f'-distort Arc 105 '
        f'/tmp/top_text_bent.png'
    )
    run_cmd(top_text_cmd)
    
    # 3. Create Bottom Text: "ONLINE EDUCATION PLATFORM" curved along bottom arc
    # Set slightly smaller font size to fit long text
    bottom_text_cmd = (
        f'convert -size 460x80 xc:transparent '
        f'-fill "{GOLD}" -font Nimbus-Roman-Bold -pointsize 22 -gravity center -annotate +0+0 "ONLINE EDUCATION PLATFORM" '
        f'-distort Arc 145 -rotate 180 '
        f'/tmp/bottom_text_bent.png'
    )
    run_cmd(bottom_text_cmd)
    
    # 4. Composite top and bottom text onto base canvas
    # Top text goes at Y=30 (centered horizontally), bottom text goes at Y=410
    composite_cmd = (
        f'composite -gravity north -geometry +0+40 /tmp/top_text_bent.png /tmp/base_canvas.png /tmp/canvas_with_top.png && '
        f'composite -gravity south -geometry +0+42 /tmp/bottom_text_bent.png /tmp/canvas_with_top.png /tmp/canvas_with_all_text.png'
    )
    run_cmd(composite_cmd)
    
    # 5. Draw Academic Elements in Center (on the beige background, within 180px radius of 256,256)
    # We draw:
    # A) An open book at Y=285 to Y=330
    # B) A graduation cap (mortarboard diamond & skull cap) centered at Y=210
    # C) Symmetrical studio headphones surrounding the mortarboard
    graphic_cmd = (
        f'convert /tmp/canvas_with_all_text.png '
        # Open Book lines/pages
        f'-fill "#FFFFFF" -stroke "{NAVY_BLUE}" -strokewidth 3 '
        f'-draw "path \'M 256,335 C 220,315 180,315 156,330 L 156,300 C 180,285 220,285 256,305 Z\'" ' # Left page
        f'-draw "path \'M 256,335 C 292,315 332,315 356,330 L 356,300 C 332,285 292,285 256,305 Z\'" ' # Right page
        # Lines in left page
        f'-stroke "{NAVY_BLUE}" -strokewidth 1.5 '
        f'-draw "line 170,307 240,296" '
        f'-draw "line 170,315 240,304" '
        f'-draw "line 170,323 240,312" '
        # Lines in right page
        f'-draw "line 272,296 342,307" '
        f'-draw "line 272,304 342,315" '
        f'-draw "line 272,312 342,323" '
        # Graduation Cap skull cap
        f'-fill "{NAVY_BLUE}" -stroke "{GOLD}" -strokewidth 3.5 '
        f'-draw "path \'M 210,230 L 210,250 C 210,272 302,272 302,250 L 302,230 Z\'" '
        # Graduation Cap top diamond
        f'-fill "{NAVY_BLUE}" -stroke "{GOLD}" -strokewidth 4 '
        f'-draw "path \'M 256,180 L 346,210 L 256,240 L 166,210 Z\'" '
        # Tassel (Gold band & dangling tassel)
        f'-fill "{GOLD}" -stroke "{GOLD}" -strokewidth 1 '
        f'-draw "circle 256,210 256,213" ' # Center button
        f'-draw "line 256,210 186,225" ' # String
        f'-draw "path \'M 186,225 L 180,240 L 192,240 Z\'" ' # Fringe
        # Headphone Headband arc surrounding cap
        f'-fill none -stroke "{GOLD}" -strokewidth 8 '
        f'-draw "path \'M 148,255 A 115,115 0 0,1 364,255\'" '
        f'-fill none -stroke "{NAVY_BLUE}" -strokewidth 4 '
        f'-draw "path \'M 148,255 A 115,115 0 0,1 364,255\'" '
        # Left and Right Earcups
        f'-fill "{NAVY_BLUE}" -stroke "{GOLD}" -strokewidth 3 '
        f'-draw "roundrectangle 132,235 152,275 6,6" '
        f'-draw "roundrectangle 360,235 380,275 6,6" '
        f'/tmp/launcher_icon.png'
    )
    run_cmd(graphic_cmd)
    print("Master icon successfully created at /tmp/launcher_icon.png")

if __name__ == "__main__":
    main()
