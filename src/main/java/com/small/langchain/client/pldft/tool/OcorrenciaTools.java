package com.small.langchain.client.pldft.tool;

import com.small.langchain.client.llm.tool.ToolRegistry;
import org.springframework.stereotype.Component;

/**
 * Monta os conjuntos de tools de cada fluxo. Expor "todas as ferramentas" em todo lugar seria o
 * caminho facil e o errado: quanto mais tools na requisicao, maior o prompt e mais chance do
 * modelo escolher a errada. Cada fluxo recebe so o que precisa.
 */
@Component
public class OcorrenciaTools {

    private final PepConsultaTool pepConsultaTool;
    private final PemConsultaTool pemConsultaTool;
    private final FuncionarioConsultaTool funcionarioConsultaTool;
    private final MovimentoConsultaTool movimentoConsultaTool;
    private final AlertaConsultaTool alertaConsultaTool;
    private final HistoricoOcorrenciaTool historicoOcorrenciaTool;

    OcorrenciaTools(
            PepConsultaTool pepConsultaTool,
            PemConsultaTool pemConsultaTool,
            FuncionarioConsultaTool funcionarioConsultaTool,
            MovimentoConsultaTool movimentoConsultaTool,
            AlertaConsultaTool alertaConsultaTool,
            HistoricoOcorrenciaTool historicoOcorrenciaTool
    ) {
        this.pepConsultaTool = pepConsultaTool;
        this.pemConsultaTool = pemConsultaTool;
        this.funcionarioConsultaTool = funcionarioConsultaTool;
        this.movimentoConsultaTool = movimentoConsultaTool;
        this.alertaConsultaTool = alertaConsultaTool;
        this.historicoOcorrenciaTool = historicoOcorrenciaTool;
    }

    /**
     * So o enquadramento cadastral (PEP, PEM, funcionario), usado com a politica que insiste
     * ate as tres responderem -- sao dados obrigatorios pro parecer.
     */
    public ToolRegistry enquadramento() {
        return ToolRegistry.of(pepConsultaTool, pemConsultaTool, funcionarioConsultaTool);
    }

    /** Tudo que o assistente do analista pode consultar durante a conversa. */
    public ToolRegistry contextoCompleto() {
        return ToolRegistry.of(
                pepConsultaTool, pemConsultaTool, funcionarioConsultaTool,
                movimentoConsultaTool, alertaConsultaTool, historicoOcorrenciaTool);
    }

    /** Fatos objetivos do caso, sem o enquadramento cadastral: movimentos, alertas e reincidencia. */
    public ToolRegistry fatosDoCaso() {
        return ToolRegistry.of(movimentoConsultaTool, alertaConsultaTool, historicoOcorrenciaTool);
    }
}
