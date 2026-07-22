# Generate a comprehensive charset file containing Latin, Cyrillic, Greek, and common symbols.
ranges = [
    (32, 126),       # Basic ASCII
    (160, 255),     # Latin-1 Supplement
    (256, 383),     # Latin Extended-A
    (384, 591),     # Latin Extended-B
    (880, 1023),    # Greek and Coptic
    (1024, 1279),   # Cyrillic
    (1280, 1327),   # Cyrillic Supplement
    (8200, 8300)    # General Punctuation & Currency Symbols
]

chars = []
for start, end in ranges:
    for codepoint in range(start, end + 1):
        chars.append(chr(codepoint))

# Also add common Polish, Ukrainian, Russian, and Czech characters explicitly
extra = "ąćęłńóśźżĄĆĘŁŃÓŚŹŻЁёЄєІіЇїҐґ"
for char in extra:
    if char not in chars:
        chars.append(char)

# Write to file in UTF-8
with open("custom_charset.txt", "w", encoding="utf-8") as f:
    f.write("".join(chars))

print("Generated custom_charset.txt with", len(chars), "characters")
