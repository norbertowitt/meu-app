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

                    bat 'git branch -a'

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

        // 🔨 Build
        stage('Build') {
            steps {
                bat 'gradlew.bat clean build'
            }
        }

        // 🧪 Testes
        stage('Test') {
            steps {
                bat 'gradlew.bat test'
            }
        }

        // 🚀 Version + Deploy (SÓ MAIN)
        stage('Version + Deploy') {

            // ✅ CORREÇÃO DEFINITIVA
            when {
                expression {
                    return env.GIT_BRANCH?.contains('main')
                }
            }

            steps {
                script {

                    echo "🚀 Entrou no Version + Deploy"

                    bat 'git fetch --tags'

                    def mensagemCommit = bat(
                        script: "git log -1 --pretty=%B",
                        returnStdout: true
                    ).trim()

                    def encontrouRelease = mensagemCommit =~ /release\/(\d+\.\d+\.\d+)/

                    def listaTags = bat(
                        script: "git tag",
                        returnStdout: true
                    ).trim()

                    listaTags = listaTags ? listaTags.split("\n") : []
                    listaTags = listaTags.findAll { it ==~ /\d+\.\d+\.\d+/ }

                    listaTags.sort { a, b ->
                        def va = a.tokenize('.').collect { it.toInteger() }
                        def vb = b.tokenize('.').collect { it.toInteger() }
                        va[0] <=> vb[0] ?: va[1] <=> vb[1] ?: va[2] <=> vb[2]
                    }

                    def ultimaVersao = listaTags ? listaTags.last() : null
                    def novaVersao

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

                    if (listaTags.contains(novaVersao)) {
                        def u = novaVersao.tokenize('.').collect { it.toInteger() }
                        novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                    }

                    def tagImagem = novaVersao

                    echo "----------------------------------------"
                    echo "Versão final: ${novaVersao}"
                    echo "Imagem: ${REGISTRY}/${NOME_IMAGEM}:${tagImagem}"
                    echo "----------------------------------------"

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

                    def caminhoJar = powershell(
                        script: "Get-ChildItem build/libs/*.jar | Select-Object -First 1 -ExpandProperty FullName",
                        returnStdout: true
                    ).trim()

                    bat "scp ${caminhoJar} user@${SERVIDOR_DOCKER}:/home/user/app.jar"

                    bat """
                    ssh user@${SERVIDOR_DOCKER} ^
                    "docker build -t ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} ^
                                  -t ${REGISTRY}/${NOME_IMAGEM}:latest /home/user &&
                     docker push ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} &&
                     docker push ${REGISTRY}/${NOME_IMAGEM}:latest"
                    """

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