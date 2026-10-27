import { useToast } from 'primevue/usetoast'

export function useNotificar() {
  const toast = useToast()
  return {
    sucesso: (detail, summary = 'Concluído') => toast.add({ severity: 'success', summary, detail, life: 3500 }),
    aviso: (detail, summary = 'Atenção') => toast.add({ severity: 'warn', summary, detail, life: 7000 }),
    erro: (e, summary = 'Não foi possível concluir') => toast.add({
      severity: 'error',
      summary,
      detail: [e?.message || String(e), ...(e?.detalhes || [])].join('\n'),
      life: 6000
    })
  }
}
