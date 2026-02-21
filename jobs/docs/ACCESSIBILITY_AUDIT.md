# Accessibility audit (frontend jobs search)

## Ferramenta e escopo
- Auditoria inicial com checklist WCAG 2.1 AA + validação manual de teclado/foco.
- Escopo: `jobs/src/main/resources/static/index.html`, `app.js` e `styles.css`.

## Ajustes aplicados
- Skip link para salto direto aos resultados.
- Foco visível padronizado para inputs, botões, links e cards.
- Melhoria de contraste em textos secundários e botões.
- Região de resultados focável (`tabindex="-1"`) para navegação assistida.

## Pendências recomendadas
- Rodar axe-core/Pa11y em CI para regressão automática.
- Teste com leitor de tela (NVDA/VoiceOver) para avaliar leitura de cartões e modal.
- Revisão de linguagem inclusiva e consistência de labels de formulário.
