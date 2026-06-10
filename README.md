
# 🧁 Brigo KDS (Kitchen Display System)

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Database](https://img.shields.io/badge/Database-Database?style=for-the-badge&logo=databricks&logoColor=white&color=00758F)
![IHC](https://img.shields.io/badge/IHC-Design_Centrado_no_Usuário-pink?style=for-the-badge)

O **Confeitaria KDS** é um sistema de exibição de cozinha desenvolvido para otimizar o fluxo de trabalho de uma confeitaria. O foco principal do projeto é a **rastreabilidade dos pedidos** (desde a montagem até a finalização) e a **experiência do confeiteiro (IHC)**, garantindo que a equipe de produção visualize as ordens de forma clara, rápida e intuitiva.

---

## 🎯 Objetivo do Projeto

Substituir os comandas manuais e papeis de pedidos por uma tela digital inteligente que organiza a fila de produção de doces e bolos, minimizando erros de comunicação e atrasos.

---

## 🛠️ Tecnologias Utilizadas

* **Linguagem:** Java versão 17
* **Interface Gráfica (IHC):** JavaFX
* **Banco de Dados:** MySQL
* **Persistência/Drivers:** JDBC (Puro)
* **Gerenciamento de Dependências:** Maven

---

## 🧠 Pilares do Desenvolvimento

### 1. ☕ Java (Core & Lógica)
O coração do sistema foi construído utilizando os conceitos de Programação Orientada a Objetos (POO). 
* Gerenciamento de fila de prioridades (ex: bolos de festa têm prioridade de horário).
* Atualização de status em tempo real.

### 2. 👥 IHC (Interação Humano-Computador)
Como o ambiente de uma cozinha de confeitaria é agitado e as mãos dos confeiteiros podem estar ocupadas ou sujas, a interface foi projetada pensando em:
* **Alta Visibilidade:** Cores contrastantes para indicar o status do pedido (ex: Verde = Novo, Amarelo = Em preparo, Vermelho = Atrasado).
* **Layout Limpo:** Fontes grandes e botões de clique fácil.
* **Acessibilidade Cognitiva:** Organização dos cards de pedidos por ordem cronológica de entrega.

### 3. 🗄️ Banco de Dados
Modelagem robusta para garantir a consistência dos dados. O banco armazena:
* Histórico de pedidos finalizados.
* Status atual de cada item da cozinha.

---

## 🚀 Funcionalidades Principais

* [ ] **Visualização em Cards:** Cada pedido é exibido como um card contendo itens, observações (ex: "Sem lactose") e horário limite.
* [ ] **Alteração de Status:** Mudança rápida de status (Pendente ➡️ Em Preparo ➡️ Pronto para Entrega).
* [ ] **Filtros de Produção:** Separar o que é da área de "Fornos" (bolos) do que é da área de "Confeiteiros" (recheios/decorações).
* [ ] **Relatório de Desempenho:** Armazenamento no banco do tempo que cada pedido levou para ser feito.

---
## Como Executar o Projeto

1. **Configurar o Banco de Dados:**
   - Execute o script SQL localizado em `src/main/resources/schema.sql` no seu servidor MySQL. Ele criará o banco `confeitaria_kds` e as tabelas necessárias, além de inserir dados de exemplo.
   - Edite o arquivo `src/main/resources/database.properties` com as credenciais do seu banco de dados (usuário e senha).

2. **Compilar e Executar via Maven:**
   ```bash
   mvn clean compile
   mvn exec:java
   ```

## Estrutura de Pacotes

- `com.kds.confeitaria.model`: Classes de entidade e enums (POO puro).
- `com.kds.confeitaria.dao`: Classes de acesso a dados (JDBC).
- `com.kds.confeitaria.service`: Lógica de negócio, fila de prioridades e regras do sistema.
- `com.kds.confeitaria.controller`: Controladores das telas JavaFX.
- `com.kds.confeitaria.util`: Utilitários (ex: conexão com o banco).
- `src/main/resources/fxml`: Arquivos de layout da interface.
- `src/main/resources/css`: Folhas de estilo (IHC).


## 📊 Arquitetura e Banco de Dados
1.usuarios

Diferencia quem está acessando o sistema para abrir a tela correta.

    id (INT, PK)

    username (VARCHAR)

    senha (VARCHAR)

    perfil (VARCHAR) – Exemplos: 'CHEF' ou 'CLIENTE'. (O Java lê isso no login para decidir qual tela abrir).

2.cardapio

Alimenta a tela do cliente e serve de base para os preços.

    id (INT, PK)

    nome (VARCHAR) – Ex: "Slice Cake de Red Velvet"

    valor (DECIMAL)

    foto_path (VARCHAR) – O caminho/diretório onde a imagem está salva (ex: "/imagens/red_velvet.png"). Evite salvar a imagem direto no banco para não deixá-lo lento.

3.pedidos (Fluxo de Pedidos)

Controla o ciclo de vida do pedido. O Chef altera essa tabela, e o Cliente apenas assiste.

    id (INT, PK)

    cliente_nome (VARCHAR) – Para aparecer no telão de retirada.

    status (VARCHAR) – Estritamente: 'RECEBIDO', 'EM_PREPARO' ou 'PRONTO'.

    data_hora (TIMESTAMP)

4.itens_pedido

Faz a ligação de quais doces estão dentro daquele pedido.

    id (INT, PK)

    pedido_id (INT, FK -> pedidos.id)

    cardapio_id (INT, FK -> cardapio.id)

    quantidade (INT)

5.vendas (Fluxo de Vendas)

Registra a movimentação monetária. Toda vez que um pedido é finalizado, uma linha nasce aqui.

    id (INT, PK)

    pedido_id (INT, FK -> pedidos.id)

    valor_total (DECIMAL)

    metodo_pagamento (VARCHAR) – Ex: 'PIX', 'Cartão'.

    data_venda (TIMESTAMP)
