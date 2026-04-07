import os
import json

fonts_dir = r"c:\Users\barte\Desktop\halo\src\client\resources\assets\halo\fonts"
font_dir = r"c:\Users\barte\Desktop\halo\src\client\resources\assets\halo\font"

if not os.path.exists(font_dir):
    os.makedirs(font_dir)

for file in os.listdir(fonts_dir):
    if file.endswith(".ttf"):
        name = file[:-4]
        json_path = os.path.join(font_dir, f"{name}.json")
        data = {
            "providers": [
                {
                    "type": "ttf",
                    "file": f"halo:fonts/{file}",
                    "shift": [0.0, 0.0],
                    "size": 11.0,
                    "oversample": 4.0
                }
            ]
        }
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)
print("Done!")
