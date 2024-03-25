pipeline {
    agent {
        node {
            label 'AGENT-1'
        }
    }

     environment { 
        packageVersion = ''
        nexusURL = '172.31.93.195:8081'
    }

    options {
        ansiColor('xterm')
        timeout(time: 1, unit: 'HOURS') 
        disableConcurrentBuilds() //It wont allow two builds at a time
    }


    parameters {
    
            booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
    
    // //     string(name: 'PERSON', defaultValue: 'Mr Jenkins', description: 'Who should I say hello to?')
    // //     text(name: 'BIOGRAPHY', defaultValue: '', description: 'Enter some information about the person')    
    // //     choice(name: 'CHOICE', choices: ['One', 'Two', 'Three'], description: 'Pick something')
    // //     password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'Enter a password')
    }
   

   
    //BUILD
    stages {       
        
        stage('Get the version') {
            steps {
                script {
                    def packageJson = readJSON file: 'package.json'
                    packageVersion = packageJson.version
                    echo "application version: ${packageVersion}"
                }
                    
            }
        }

        stage('Install dependencies') {
            steps {
                script {
                    sh """
                        npm install
                    """
                }
                    
            }
        }
                
        stage('Build') {
            steps {
                sh """
                    ls -la
                    zip -q -r catalogue.zip ./* -x ".git" -x "*.zip"
                    ls -ltr
                """
            }
        }

        stage('Unit Test') {
            steps {
                sh """
                    echo "Unit test will happen here"
                """
            }
        }

        stage('Sonar-scan') {
            steps {
                sh """
                    sonar-scanner
                """
            }
        }

        
        stage('Publish Artifact') {
            steps {
                nexusArtifactUploader(
                    nexusVersion: 'nexus3',
                    protocol: 'http',
                    nexusUrl: "${nexusURL}",
                    groupId: 'com.roboshop',
                    version: "${packageVersion}",
                    repository: 'catalogue',
                    credentialsId: 'nexus-auth',
                    artifacts: [
                        [artifactId: 'catalogue',
                        classifier: '',
                        file: 'catalogue.zip',
                        type: 'zip']
                    ]
                )
                
            }
     
        }

        stage('Deploy') {
            
            when {
                expression {
                    params.Deploy == 'true'
                }

            }
            
            steps {
               build job: 'catalogue-deploy', wait: true, 
               parameters: [string(name: 'version', value: "${packageVersion}"), 
               string(name: 'environment', value: "dev") 
               ]
            }     
        }
       

    }
    // POST BUILD
    post { 
        always { 
            echo 'I will always say Hello again!'
            deleteDir()
        }

        failure { 
            echo 'this runs when pipeline is failed, used generally to send alerts'
        }
        success { 
            echo 'I will always say Hello when pipeline is success'
        }
    }
}