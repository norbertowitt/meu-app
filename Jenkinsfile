pipeline {
    agent any

    environment {
        SERVIDOR_DOCKER = "192.168.0.100"
        SERVIDOR_K8S = "192.168.0.110"
        USUARIO_SSH = "norbertoneto"
        NOME_IMAGEM = "meu-app"
        REGISTRY = "192.168.0.100:5000"
        GRADLE_USER_HOME = "${WORKSPACE}\\.gradle"
    }

    stages {

        stage('Checkout') {
            steps {
                echo "📥 Iniciando checkout do repositório..."
                checkout scm
                echo "✅ Checkout realizado com sucesso."
            }
        }

        stage('DEBUG - Branch Info') {
            steps {
                script {
                    echo "🔍 Verificando informações da branch..."
                    bat(script: 'git branch -a')

                    def branchGit = bat(
                        script: '@git rev-parse --abbrev-ref HEAD',
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

        stage('Build') {
            steps {
                echo "🔨 Build..."
                bat(script: 'gradlew.bat clean bootJar --no-daemon')
            }
        }

        stage('Test') {
            steps {
                echo "🧪 Testes..."
                bat(script: 'gradlew.bat test --no-daemon')
            }
        }

        stage('Version + Deploy') {

            when {
                expression { return env.GIT_BRANCH?.contains('main') }
            }

            steps {
                script {

                    echo "🚀 Version + Deploy"

                    bat(script: 'git fetch --tags --force')

                    def mensagemCommit = bat(
                        script: '@git log -1 --pretty=%%B',
                        returnStdout: true
                    ).trim()

                    def versaoRelease = (mensagemCommit =~ /release\/(\d+\.\d+\.\d+)/)?.find()
                        ? (mensagemCommit =~ /release\/(\d+\.\d+\.\d+)/)[0][1]
                        : null

                    def listaTagsRaw = bat(
                        script: '@git tag',
                        returnStdout: true
                    ).trim()

                    def listaTags = listaTagsRaw ?
                        listaTagsRaw.readLines().collect { it.trim() }.findAll { it ==~ /\d+\.\d+\.\d+/ }
                        : []

                    listaTags = listaTags.sort { a, b ->
                        def va = a.tokenize('.').collect { it.toInteger() }
                        def vb = b.tokenize('.').collect { it.toInteger() }
                        if (va[0] != vb[0]) return va[0] <=> vb[0]
                        if (va[1] != vb[1]) return va[1] <=> vb[1]
                        return va[2] <=> vb[2]
                    }

                    def ultimaVersao = listaTags ? listaTags.last() : null

                    def novaVersao

                    if (versaoRelease) {

                        def r = versaoRelease.tokenize('.').collect { it.toInteger() }
                        def u = ultimaVersao ? ultimaVersao.tokenize('.').collect { it.toInteger() } : null

                        if (!u ||
                                r[0] > u[0] ||
                                (r[0] == u[0] && r[1] > u[1]) ||
                                (r[0] == u[0] && r[1] == u[1] && r[2] > u[2])) {
                            novaVersao = versaoRelease
                        } else {
                            novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                        }

                    } else {
                        novaVersao = ultimaVersao ?
                            "${ultimaVersao.tokenize('.')[0]}.${ultimaVersao.tokenize('.')[1]}.${ultimaVersao.tokenize('.')[2].toInteger() + 1}" :
                            "1.0.0"
                    }

                    while (listaTags.contains(novaVersao)) {
                        def u = novaVersao.tokenize('.').collect { it.toInteger() }
                        novaVersao = "${u[0]}.${u[1]}.${u[2] + 1}"
                    }

                    def tagImagem = novaVersao

                    echo "🐳 Versão: ${novaVersao}"

                    withCredentials([usernamePassword(
                        credentialsId: 'github-token',
                        usernameVariable: 'GIT_USER',
                        passwordVariable: 'GIT_TOKEN'
                    )]) {

                        bat(script: 'git config user.email "jenkins@local"')
                        bat(script: 'git config user.name "Jenkins"')
                        bat(script: 'git remote set-url origin https://%GIT_USER%:%GIT_TOKEN%@github.com/norbertowitt/meu-app.git')

                        bat(script: "git tag ${novaVersao}")
                        bat(script: "git push origin ${novaVersao}")
                    }

                    def caminhoJar = powershell(
                        script: 'Get-ChildItem build/libs/*.jar | Where-Object { $_.Name -notlike "*-plain.jar" } | Select-Object -First 1 -ExpandProperty FullName',
                        returnStdout: true
                    ).trim()

                    bat(script: 'dir build\\libs')

                    // =========================
                    // SSH / SCP EM UMA LINHA
                    // =========================

                    bat(script: "scp -o StrictHostKeyChecking=no \"${caminhoJar}\" ${USUARIO_SSH}@${SERVIDOR_DOCKER}:/home/${USUARIO_SSH}/app.jar")
                    bat(script: "scp -o StrictHostKeyChecking=no Dockerfile ${USUARIO_SSH}@${SERVIDOR_DOCKER}:/home/${USUARIO_SSH}/Dockerfile")

                    bat(script: "ssh -o StrictHostKeyChecking=no ${USUARIO_SSH}@${SERVIDOR_DOCKER} \"cd /home/${USUARIO_SSH} && docker build -t ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} -t ${REGISTRY}/${NOME_IMAGEM}:latest . && docker push ${REGISTRY}/${NOME_IMAGEM}:${tagImagem} && docker push ${REGISTRY}/${NOME_IMAGEM}:latest\"")

                    bat(script: "ssh -o StrictHostKeyChecking=no ${USUARIO_SSH}@${SERVIDOR_K8S} \"argocd app sync meu-app\"")

                    echo "🎉 Finalizado"
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