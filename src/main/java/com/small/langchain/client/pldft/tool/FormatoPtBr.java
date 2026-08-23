package com.small.langchain.client.pldft.tool;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatacao das saidas das tools. O texto devolvido por uma tool vai direto pro contexto do
 * modelo, entao vale gastar um pouco de cuidado: valor e data escritos de forma consistente
 * reduzem a chance do modelo reinterpretar numero errado no parecer.
 */
final class FormatoPtBr {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormatoPtBr() {
    }

    static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    static String data(LocalDateTime dataHora) {
        return dataHora != null ? DATA.format(dataHora) : "-";
    }
}
