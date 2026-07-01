"""
Program deteksi teks & angka pada gambar (OCR) + penghitung total resource.

Cara kerja:
1. Baca gambar
2. Preprocessing (grayscale, resize, threshold) supaya teks lebih jelas dibaca OCR
3. Ekstrak teks pakai Tesseract OCR
4. Cari pola angka dengan satuan (K/M/B) memakai regex
5. Konversi satuan ke angka penuh, lalu jumlahkan

Requirement:
    pip install pytesseract pillow --break-system-packages
    (tesseract engine harus sudah terinstall di sistem)
"""

import re
import sys
from PIL import Image, ImageOps, ImageFilter, ImageDraw, ImageFont
import pytesseract


def preprocess_image(path):
    """Ubah gambar jadi grayscale + perbesar + sedikit sharpen agar OCR lebih akurat."""
    img = Image.open(path).convert("L")  # grayscale
    # perbesar 2x biar teks kecil lebih jelas terbaca
    img = img.resize((img.width * 2, img.height * 2), Image.LANCZOS)
    img = ImageOps.autocontrast(img)
    img = img.filter(ImageFilter.SHARPEN)
    return img


def extract_text(path):
    """Jalankan OCR dan kembalikan teks mentah hasil bacaan gambar."""
    img = preprocess_image(path)
    text = pytesseract.image_to_string(img)
    return text


def convert_value(num_str, suffix):
    """Konversi angka dengan suffix K/M/B jadi angka penuh (float)."""
    num = float(num_str)
    suffix = suffix.upper()
    multiplier = {
        "": 1,
        "K": 1_000,
        "M": 1_000_000,
        "B": 1_000_000_000,
    }.get(suffix, 1)
    return num * multiplier


def find_values(text):
    """
    Cari semua angka dalam teks, termasuk yang memakai suffix K/M/B
    dan angka dengan pemisah ribuan (koma/titik).
    Contoh yang terdeteksi: 19.3M, 6.1M, 764,942, 1.5M, 4,696
    """
    pattern = r"(\d[\d,]*\.?\d*)\s*([KMB]?)\b"
    matches = re.findall(pattern, text)

    results = []
    for num_str, suffix in matches:
        clean_num = num_str.replace(",", "")
        if clean_num in ("", "."):
            continue
        try:
            value = convert_value(clean_num, suffix)
            results.append({
                "raw": num_str + suffix,
                "value": value
            })
        except ValueError:
            continue
    return results


def load_font(size):
    """Coba pakai font DejaVuSans (biasanya tersedia di Linux/Termux), fallback ke font default."""
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/data/data/com.termux/files/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
    ]
    for path in candidates:
        try:
            return ImageFont.truetype(path, size)
        except (OSError, IOError):
            continue
    return ImageFont.load_default()


def append_result_panel(image_path, rows, total_from_items, total_total, output_path):
    """
    Buat gambar baru: gambar asli di atas + panel berisi hanya total
    (From Items & Total Resources), ditempel di bagian bawah, rata tengah.
    """
    original = Image.open(image_path).convert("RGB")
    width = original.width

    # Hanya tampilkan judul + 2 baris total, tanpa rincian per resource
    lines = [
        "HASIL PERHITUNGAN",
        f"Total From Items: {format_compact(total_from_items)}",
        f"Total Resources : {format_compact(total_total)}",
    ]

    font_title = load_font(30)
    font_body = load_font(26)
    fonts = [font_title, font_body, font_body]

    line_height = 42
    padding_top_bottom = 30
    panel_height = padding_top_bottom * 2 + line_height * len(lines)

    # Buat kanvas baru: tinggi gambar asli + tinggi panel
    new_img = Image.new("RGB", (width, original.height + panel_height), (20, 24, 32))
    new_img.paste(original, (0, 0))

    draw = ImageDraw.Draw(new_img)
    y = original.height + padding_top_bottom
    for i, line in enumerate(lines):
        font = fonts[i]
        color = (255, 215, 0) if i == 0 else (255, 255, 255)
        # hitung lebar teks untuk rata tengah
        bbox = draw.textbbox((0, 0), line, font=font)
        text_width = bbox[2] - bbox[0]
        x = (width - text_width) / 2
        draw.text((x, y), line, font=font, fill=color)
        y += line_height

    new_img.save(output_path)
    return output_path
    """Format angka besar biar mudah dibaca (mis. 50,500,000)."""
    return f"{n:,.0f}"


def format_compact(n):
    """
    Format angka kembali ke bentuk singkat K/M/B, bukan angka penuh dengan nol.
    Contoh: 50500000 -> 50.5M, 1100000 -> 1.1M, 764942 -> 764.9K
    """
    n = float(n)
    abs_n = abs(n)
    if abs_n >= 1_000_000_000:
        return f"{n / 1_000_000_000:.2f}".rstrip("0").rstrip(".") + "B"
    elif abs_n >= 1_000_000:
        return f"{n / 1_000_000:.2f}".rstrip("0").rstrip(".") + "M"
    elif abs_n >= 1_000:
        return f"{n / 1_000:.2f}".rstrip("0").rstrip(".") + "K"
    else:
        return f"{n:.0f}"


def parse_resource_table(text, min_value=1000):
    """
    Cari baris yang mengandung nama resource + 2 angka (From Items, Total Resources),
    lalu pisahkan jadi dua kolom.
    """
    rows = []
    for line in text.splitlines():
        nums = find_values(line)
        nums = [v for v in nums if v["value"] >= min_value]
        if len(nums) >= 2:
            # ambil nama resource: kata pertama yang bukan simbol aneh
            words = [w for w in re.findall(r"[A-Za-z]+", line) if len(w) > 2]
            name = words[-1] if words else "Unknown"
            rows.append({
                "name": name.capitalize(),
                "from_items": nums[-2]["value"],
                "total": nums[-1]["value"],
            })
    return rows


def main(image_path, min_value=1000):
    print(f"📷 Membaca gambar: {image_path}\n")

    raw_text = extract_text(image_path)
    print("=== TEKS HASIL OCR (mentah) ===")
    print(raw_text)
    print("================================\n")

    rows = parse_resource_table(raw_text, min_value)

    if not rows:
        print("Tidak ada tabel resource yang terdeteksi, coba tampilkan angka mentah:")
        for v in find_values(raw_text):
            print(f"  {v['raw']} -> {format_compact(v['value'])}")
        return

    print("=== TABEL RESOURCE TERDETEKSI ===")
    print(f"{'Resource':<10}{'From Items':>15}{'Total':>15}")
    total_from_items = 0
    total_total = 0
    for r in rows:
        print(f"{r['name']:<10}{format_compact(r['from_items']):>15}{format_compact(r['total']):>15}")
        total_from_items += r["from_items"]
        total_total += r["total"]

    print("\n=== HASIL PERHITUNGAN ===")
    print(f"Jumlah resource terdeteksi     : {len(rows)}")
    print(f"Total kolom 'From Items'       : {format_compact(total_from_items)}")
    print(f"Total kolom 'Total Resources'  : {format_compact(total_total)}")

    import os
    base_name = os.path.basename(image_path).rsplit(".", 1)[0]
    output_path = base_name + "_hasil.jpg"
    append_result_panel(image_path, rows, total_from_items, total_total, output_path)
    print(f"\n🖼️  Gambar dengan hasil perhitungan disimpan di: {output_path}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Cara pakai: python3 resource_ocr.py <path_gambar>")
        sys.exit(1)
    main(sys.argv[1])
