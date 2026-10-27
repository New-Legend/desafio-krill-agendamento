package br.com.redekrill.agendamento.infrastructure.config;

import br.com.redekrill.agendamento.application.port.Transacao;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class TransacaoSpring implements Transacao {
    private final TransactionTemplate template;

    public TransacaoSpring(TransactionTemplate template) {
        this.template = template;
    }

    @Override
    public <T> T executar(Supplier<T> operacao) {
        return template.execute(status -> operacao.get());
    }
}
