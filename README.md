# Marketplace in Cloud

**Motivation**: This is one of my side project I have worked on.

**Goal**: The whole marketplace does not require infrastructure setup and can be deployed on AWS directly with one-click.

**Current version supports**:
* APIs to list all products/inventory
* Order creation and inventory checking during Sale
* Payment integration with Stripe

**Navigation**:
Project is separated into 2 parts:
1. _functions_: contains business logic
2. _infra_: contains infrastructure code that deploys the architecture.

**Usage**
* Install and configure aws-cli and aws-cdk
  * `npm i -g aws-cdk` to install cdk tool
  * Install AWS-CLI-V2 binary from Amazon
  * `aws configure` to configure IAM/Root for cdk to use
* `mvn clean package`     compiles business logic
* `cd infra && cdk synth` compiles infra code to cloudformation template
* `cdk deploy` deploys to AWS infrastructure

**Clean up**
* `cdk destroy` roll-down and destroy infrastructure
* `rm ~/.aws/credentials` to clean-up AWS credentials.[Recommended] 


**Other useful commands**
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation

Enjoy!
