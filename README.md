# SgP
Sistema web de gestão de projetos e tarefas, iniciado na cadeira de POO e em evolução para acompanhar o Jambu.

## Executar

Requisitos: JDK 21 ou superior e PostgreSQL. O projeto usa Maven Wrapper.

```powershell
git clone https://github.com/lxvlucas/SgP.git
Set-Location SgP
```

Crie uma base PostgreSQL e configure a conexão no terminal:

```powershell
$env:PGHOST = 'localhost'
$env:PGPORT = '5432'
$env:PGDATABASE = 'sgp_db'
$env:PGUSER = 'sgp_app'
$senhaBanco = Read-Host 'Senha do banco' -AsSecureString
$env:PGPASSWORD = [Net.NetworkCredential]::new('', $senhaBanco).Password
.\mvnw.cmd spring-boot:run
```

Use o nome da base e o usuário que você criou. A aplicação fica em http://localhost:8080. Flyway cria as tabelas numa base vazia; Hibernate valida a estrutura. Bases anteriores ao Flyway exigem conferência do esquema e baseline explícito antes da atualização.

## Dados de demonstração

A configuração padrão não cria contas ou projetos de exemplo. Para usar os exemplos, ative explicitamente o perfil `demo`:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Esse perfil usa a conexão PostgreSQL configurada pelas variáveis acima. Use uma base reservada para demonstração. Conta de exemplo de gestor: `gestor@sgp.com`, senha `123456`.

## Testes

Para rodar os testes no Windows usando a instalação local do PostgreSQL 18:

```powershell
.\scripts\test-postgresql.ps1
```

O script cria e encerra uma instância temporária exclusiva de testes. Não precisa da senha do serviço instalado. Última validação: **36 testes passando no PostgreSQL 18.4**.
