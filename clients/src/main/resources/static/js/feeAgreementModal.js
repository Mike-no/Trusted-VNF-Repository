"use strict";

angular.module('devModule').controller('FeeAgreementModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const feeAgreementModal = this;

    feeAgreementModal.form = {};
    feeAgreementModal.formError = false;

    /* Validate and create a fee agreement */
    feeAgreementModal.create = () => {
        if(invalidFormInput())
            feeAgreementModal.formError = true;
        else {
            feeAgreementModal.formError = false;

            const maxAcceptableFee = feeAgreementModal.form.maxAcceptableFee;
            $uibModalInstance.close();

            /* Define the fee agreement establishment endpoint */
            const establishFeeAgreementEndpoint =
                apiBaseURL + `establish-fee-agreement?maxAcceptableFee=${maxAcceptableFee}`;
            /* Hit the endpoint to establish a fee agreement and handle success/failure responses */
            $http.put(establishFeeAgreementEndpoint).then(
                (result) => feeAgreementModal.displayMessage(result),
                (result) => feeAgreementModal.displayMessage(result)
            ).catch(function onError(error) {
                console.log(error);
            });
        }
    };

    /* Displays the success/failure response from attempting to establish a fee agreement */
    feeAgreementModal.displayMessage = (message) => {
        const feeAgreementMsgModal = $uibModal.open({
            templateUrl  : 'feeAgreementMsgModal.html',
            controller   : 'feeAgreementMsgModalCtrl',
            controllerAs : 'feeAgreementMsgModal',
            resolve : { message : () => message }
        });

        /* No behaviour on close/dismiss */
        feeAgreementMsgModal.result.then(() => {}, () => {});
    };

    /* Closes the fee agreement establishment modal */
    feeAgreementModal.cancel = () => $uibModalInstance.dismiss();

    /* Validate the fee agreement establishment */
    function invalidFormInput() {
        return (isNaN(feeAgreementModal.form.maxAcceptableFee) || feeAgreementModal.form.maxAcceptableFee < 0 ||
            feeAgreementModal.form.maxAcceptableFee > 100);
    }
});

/* Controller for the success/fail modal */
angular.module('devModule').controller('feeAgreementMsgModalCtrl', function($uibModalInstance, message) {
    const feeAgreementMsgModal = this;
    feeAgreementMsgModal.message = message.data;
});