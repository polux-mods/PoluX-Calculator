package ua.polux.smartcalc

object OcrMath {
    fun clean(text: String): String {
        var s = text
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("×", "*")
            .replace("x", "*")
            .replace("X", "*")
            .replace("·", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("—", "-")
            .replace("–", "-")
            .replace(":", "/")
            .replace("√", "sqrt")
            .replace("π", "pi")
            .replace(",", ".")
            .replace(" ", "")

        // Common OCR confusions in math photos.
        s = s.replace(Regex("""(?<=\d)[oO](?=\d|[+\-*/^().])"""), "0")
        s = s.replace(Regex("""(?<=\D)[lI](?=\d|\D|$)"""), "1")
        s = s.replace("²", "^2").replace("³", "^3")

        // Remove labels around a detected expression.
        s = s.replace(Regex("""(?i)^(answer|ans|result|розв'язок|відповідь)[:=]?"""), "")
        s = s.replace(Regex("""[^0-9a-zA-Z_+*/^%().=,\-]"""), "")
        return s
    }
}
