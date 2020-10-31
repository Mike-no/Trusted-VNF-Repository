"use strict";

angular.module('mainModule').controller('SelfIssueCashModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const selfIssueCashModal = this;

    selfIssueCashModal.form = {};
    selfIssueCashModal.formError = false;

    /* Validate and self issue an amount of a specified currency */
    selfIssueCashModal.issue = () => {
        if(invalidFormInput())
            selfIssueCashModal.formError = true;
        else {
            selfIssueCashModal.formError = false;

            const currency = selfIssueCashModal.form.currency;
            const amount = selfIssueCashModal.form.amount;
            $uibModalInstance.close();

            /* Define the self issue cash endpoint */
            const selfIssueCashEndpoint = apiBaseURL + `self-issue-cash?amount=${amount}&currency=${currency}`;
            /* Hit the endpoint to self issue cash and handle success/failure responses */
            $http.post(selfIssueCashEndpoint).then(
                (result) => selfIssueCashModal.displayMessage(result),
                (result) => selfIssueCashModal.displayMessage(result)
            ).catch(function onError(error) {
                console.log(error);
            });
        }
    };

    /* Displays the success/failure response from attempting to self issue cash */
    selfIssueCashModal.displayMessage = (message) => {
        const selfIssueCashMsgModal = $uibModal.open({
            templateUrl  : 'selfIssueCashMsgModal.html',
            controller   : 'selfIssueCashMsgModalCtrl',
            controllerAs : 'selfIssueCashMsgModal',
            resolve      : { message : () => message }
        });

        /* No behaviour on close/dismiss */
        selfIssueCashMsgModal.result.then(() => {}, () => {});
    };

    /* Closes the self issue cash modal */
    selfIssueCashModal.cancel = () => $uibModalInstance.dismiss();

    /* Validate the self issue cash */
    function invalidFormInput() {
        return isNaN(selfIssueCashModal.form.amount) || selfIssueCashModal.form.amount <= 0 ||
            (selfIssueCashModal.form.currency.length != 3);
    }
});

/* Controller for the success/fail modal */
angular.module('mainModule').controller('selfIssueCashMsgModalCtrl', function($uibModalInstance, message) {
    const selfIssueCashMsgModal = this;
    selfIssueCashMsgModal.message = message.data;
});