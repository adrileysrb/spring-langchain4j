package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.pldft.model.Movimento;
import com.small.langchain.client.pldft.model.Ocorrencia;
import com.small.langchain.client.pldft.repository.MovimentoRepository;
import com.small.langchain.client.pldft.repository.OcorrenciaRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Tool que resume as movimentacoes financeiras da pessoa monitorada de uma ocorrencia.
 *
 * <p>O resumo e calculado em Java de proposito: somar dezenas de lancamentos e exatamente o
 * tipo de tarefa em que um LLM erra, entao a tool entrega o numero pronto e deixa pro modelo
 * so a parte que ele faz bem -- interpretar o padrao e escrever sobre ele.
 */
@Component
public class MovimentoConsultaTool {

    private static final String CREDITO = "CREDITO";

    private final OcorrenciaRepository ocorrenciaRepository;
    private final MovimentoRepository movimentoRepository;

    MovimentoConsultaTool(OcorrenciaRepository ocorrenciaRepository, MovimentoRepository movimentoRepository) {
        this.ocorrenciaRepository = ocorrenciaRepository;
        this.movimentoRepository = movimentoRepository;
    }

    @Tool("Resume as movimentacoes financeiras (creditos e debitos) da pessoa monitorada de uma " +
            "ocorrencia de PLDFT: quantidade de lancamentos, totais, maior valor e periodo. " +
            "Use o id numerico da ocorrencia. Os valores ja vem somados e conferidos, use-os " +
            "como estao em vez de recalcular.")
    public String consultarMovimentacoes(@P("id da ocorrencia") long ocorrenciaId) {
        Ocorrencia ocorrencia = ocorrenciaRepository.findById(ocorrenciaId).orElse(null);
        if (ocorrencia == null) {
            return "Ocorrência #" + ocorrenciaId + " não encontrada.";
        }

        List<Movimento> movimentos = movimentoRepository.findByPessoaMonitoradaId(ocorrencia.pessoaMonitoradaId());
        if (movimentos.isEmpty()) {
            return "Nenhuma movimentação registrada para a pessoa monitorada da ocorrência #" + ocorrenciaId + ".";
        }

        List<Movimento> creditos = movimentos.stream().filter(m -> CREDITO.equals(m.tipoLancamento())).toList();
        List<Movimento> debitos = movimentos.stream().filter(m -> !CREDITO.equals(m.tipoLancamento())).toList();

        BigDecimal totalCreditos = somar(creditos);
        BigDecimal totalDebitos = somar(debitos);

        return "Movimentações da pessoa monitorada da ocorrência #" + ocorrenciaId
                + " | Período: " + FormatoPtBr.data(movimentos.getFirst().dataMovimento())
                + " a " + FormatoPtBr.data(movimentos.getLast().dataMovimento())
                + " | Créditos: " + creditos.size() + " lançamento(s), total " + FormatoPtBr.moeda(totalCreditos)
                + ", maior " + FormatoPtBr.moeda(maiorValor(creditos))
                + " | Débitos: " + debitos.size() + " lançamento(s), total " + FormatoPtBr.moeda(totalDebitos)
                + ", maior " + FormatoPtBr.moeda(maiorValor(debitos))
                + " | Saldo líquido: " + FormatoPtBr.moeda(totalCreditos.subtract(totalDebitos));
    }

    private BigDecimal somar(List<Movimento> movimentos) {
        return movimentos.stream().map(Movimento::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal maiorValor(List<Movimento> movimentos) {
        return movimentos.stream().map(Movimento::valor).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }
}
