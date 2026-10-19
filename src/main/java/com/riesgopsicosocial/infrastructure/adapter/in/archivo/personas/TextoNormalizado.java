package com.riesgopsicosocial.infrastructure.adapter.in.archivo.personas;

import java.text.Normalizer;
import java.util.Locale;

/** Para comparar lo que viene del archivo: sin tildes, mayúsculas, espacios ni diferencias entre guiones. */
final class TextoNormalizado {

    private TextoNormalizado() {
    }

    static String de(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[\\u2010-\\u2015\\u0096]", "-")
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

}
