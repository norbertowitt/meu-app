pipeline {
    agent any

    // 🌎 Variáveis de ambiente (configuração geral)
    environment {
        SERVIDOR_DOCKER = "192.168.0.100"
        SERVIDOR_K8S = "192.168.0.110"
        NOME_IMAGEM = "meu-app"
        REGISTRY = "192.168.0.100:5000"
    }

    stages {

        // 📥 Baixa o código do repositório
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // 🔍 DEBUG: mostra qual branch o Jenkins realmente está usando
        stage('DEBUG - Branch Info') {
            steps {
                script {

                    // lista branches locais e remotas
                    bat 'git branch -a'

                    // pega branch atual do git
                    def branchGit = bat(
                        script: "git rev-parse --abbrev-ref HEAD",
                        returnStdout: true
                    ).trim()

                    echo "=============================="
                    echo "BRANCH (Git): ${branchGit}"
                    echo "BRANCH_NAME (Jenkins): ${env.BRANCH_NAME}"
                    echo "GIT_BRANCH (Jenkins): ${env.GIT_BRANCH}"
                    echo "=============================="
                }
            }
        }

        // 🔨 Compila o projeto
        stage('Build') {
            steps {
                bat 'gradlew.bat clean build'
            }
        }

        // 🧪 Executa os testes
        stage('Test') {
            steps {
                bat 'gradlew.bat test'
            }
        }

        // 🚀 Versão + Deploy (SÓ MAIN)
        stage('Version + Deploy') {

            // 🔥 CORREÇÃO IMPORTANTE AQUI
            when {
                expression {
                    return env.BRANCH_NAME == 'main'
                }
            }

            steps {
                script {

                    echo "🚀 Entrou no stage Version + Deploy"

                    // 🔄 Atualiza tags do Git
                    bat 'git fetch --tags'

                    // 📌 Último commit
                    def mensagemCommit = bat(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    // 📌 Procura release/x.y.z
                    def encontrouRelease = mensagemCommit =~ /release\/(\d+\.\d+\.\d+)/

                    // 📌 Lista tags
                    def listaTags = bat(
                        script: "git tag",
                        returnStdout: true
                    ).trim()

                    listaTags = listaTags ? listaTags.split("\n") : []
                    listaTags = listaTags.findAll { it ==~ /\d+\.\d+\.\d+/ }

                    // 📌 Ordena versões
                    listaTags.sort { a, b ->
                        def va = a.tokenize('.').collect { it.toInteger() }
                        def vb = b.tokenize('.').collect { it.toInteger() }
                        va[0] <=> vb[0] ?: va[1] <=> vb[1] ?: va[2] <=> vb[2]
                    }

                    def ultimaVersao = listaTags ? listaTags.last() : null
                    def novaVersao

                    // 🧠 Regra de versão
                    if (encontrouRelease) {

                        def versaoRelease = encontrouRelease[0][1]

                        if (!ultimaVersao) {
                            novaVersao = versaoRelease
                        } else {
                            def r = versaoRelease.tokenize('.').collect { it.toInteger() }
                            def u = ultimaVersao.tokenize('.').collect { it.toInteger() }

                            if (r[0] > u[0] || (r[0] == u[0] && r[1] > u[1])) {
                                novaVersao = versaoRelease
                            } else {
                                novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                            }
                        }

                    } else {

                        if (!ultimaVersao) {
                            novaVersao = "1.0.0"
                        } else {
                            def u = ultimaVersao.tokenize('.').collect { it.toInteger() }
                            novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                        }
                    }

                    // 🔴 evita duplicar tag
                    if (listaTags.contains(novaVersao)) {
                        echo "Tag já existe, incrementando..."

                        def u = novaVersao.tokenize('.').collect { it.toInteger() }
                        novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                    }

                    def tagImagem = novaVersao

                    echo "----------------------------------------"
                    echo "Versão final: ${novaVersao}"
                    echo "Imagem: ${REGISTRY}/${NOME_IMAGEM}:${tagImagem}"
                    echo "----------------------------------------"

                    // 🔐 CRIA TAG NO GIT
                    withCredentials([usernamePassword(
                        credentialsId: 'github-token',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_TOKEN'
                    )]) {

                        bat """
                        git config user.email "jenkins@local"
                        git config user.name "Jenkins"

                        git remote set-url origin https://%GIT_USER%:%GIT_TOKEN%@github.com/norbertowitt/meu-app.git

                        git tag ${novaVersao}
                        git push origin ${novaVersao}
                        """
                    }

                    // 📦 Encontra o .jar
                    def caminhoJar = powershell(
                        script: "Get-ChildItem build/libs/*.jar | Select-Object -First 1 -ExpandProperty FullName",
                        returnStdout: true
                    ).trim()

                    // 📤 Envia pro servidor Docker
                    bat "scp ${caminhoJar} user@${SERVIDOR_DOCKER}:/home/user/app.jar"

                    // 🐳 Build + push registry
                    bat """
                    ssh user@${SERVIDOR_DOCKER} ^
                    "docker build -t ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} ^
                                  -t ${REGISTRY}/${NOME_IMAGEM}:latest /home/user &&
                     docker push ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} &&
                     docker push ${REGISTRY}/${NOME_IMAGEM}:latest"
                    """

                    // ☸️ Deploy Argo CD
                    bat """
                    ssh user@${SERVIDOR_K8S} ^
                    "argocd app sync meu-app"
                    """
                }
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/test/*.xml'
        }
    }
}