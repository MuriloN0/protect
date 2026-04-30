# 🛡️ Projeto Integrador: Sistema de Segurança e Gestão de Credenciais

Este projeto é o resultado do **Projeto Integrador de Políticas de Segurança da Informação**. O objetivo central é o desenvolvimento de uma aplicação prática que articula conceitos de autenticação segura, comunicação criptografada e gestão de credenciais, mantendo conformidade estrita com a **LGPD (Lei Geral de Proteção de Dados)**.

---

## 🚀 Tecnologias Utilizadas

* **Linguagem:** Java 17
* **Framework:** Spring Boot 3
* **Segurança:** Spring Security & JWT (Auth0)
* **Criptografia:** BCrypt (Work factor 12)
* **Comunicação:** Spring Mail (SMTP) para 2FA e recuperação
* **Arquitetura:** REST API (Stateless) preparada para integração com Angular

---

## 📑 Entregas do Projeto (Roadmap)

### 📍 Autenticação e Gestão de Credenciais
* **Autenticação em Duas Etapas (2FA):** O login inicial valida as credenciais e dispara um código temporário de 6 dígitos via e-mail.
* **Proteção Brute Force:** Bloqueio temporário de conta após 5 tentativas falhas consecutivas (15 minutos de cooldown).
* **Gestão de Sessão:** Endpoint de `/logout` que invalida o token no servidor através de uma `Blacklist` de tokens revogados.

### 📍 Recuperação de Senha
* **Fluxo Seguro:** Solicitação de redefinição via e-mail com token UUID de validade curta (15 min).
* **Anti-Enumeração:** A API não confirma se um e-mail existe na base durante a solicitação, protegendo a privacidade dos usuários.

### 📍 Criptografia e Comunicação
* **Hashing:** Armazenamento de senhas utilizando BCrypt.
* **JWT:** Emissão de tokens assinados com algoritmo HMAC256 para autorização de rotas protegidas.

---

## 🔒 Requisitos do Sistema (Referente à Entrega 1)

### Requisitos Funcionais (RF)
1.  **RF01 - Exclusão de Conta (LGPD):** Permitir ao usuário a deleção total de seus dados pessoais.
2.  **RF02 - Exportação de Dados:** Funcionalidade de portabilidade para o usuário baixar seus dados em JSON.
3.  **RF03 - Auditoria de Acesso:** Registro em log de todas as tentativas de login (IP, Data, Sucesso/Falha).
4.  **RF04 - Notificação de Login:** Envio de e-mail ao detectar acesso por novo dispositivo.
5.  **RF05 - Gestão de Consentimento:** Registro da data e versão dos termos de uso aceitos pelo usuário.
6.  **RF06 - Invalidar Sessões:** Ao trocar a senha, o sistema deve deslogar todos os outros dispositivos ativos.

### Requisitos Não-Funcionais (RNF)
7.  **RNF01 - Criptografia em Repouso:** Dados sensíveis devem ser criptografados na base de dados.
8.  **RNF02 - Política de Senhas

---

## ⚙️ Como Executar
* Clone o repositório.
* Configure as credenciais do banco de dados e do servidor SMTP no arquivo src/main/resources/application.properties.
* Adicione a chave secreta do JWT na variável de ambiente correspondente ou no properties (api.security.token.secret).
* Execute o comando mvn spring-boot:run.
* A API estará disponível na porta 8080 para ser consumida pela aplicação front-end.
