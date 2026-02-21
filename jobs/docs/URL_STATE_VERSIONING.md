# Governança de versão do estado de URL

O frontend mantém `URL_STATE_VERSION` e `URL_STATE_MIGRATIONS` em `app.js`.

## Regras
1. Toda mudança incompatível no schema de URL deve incrementar `URL_STATE_VERSION`.
2. A migração da versão anterior deve ser adicionada em `URL_STATE_MIGRATIONS`.
3. Deve existir fallback para estado salvo em `localStorage` legado.
4. Em PR de mudança de schema, incluir teste/manual de backward compatibility.

## Versões
- v1: payload legado sem query params versionados.
- v2: query params versionados (`?v=2`) e chips serializados.
- v3: formalização de migrações e `schemaVersion` persistido.
