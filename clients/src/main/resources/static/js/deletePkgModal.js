"use strict";

angular.module('devModule').controller('DeletePkgModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL, linearId) {
    const deletePkgModal = this;

    deletePkgModal.deletePkg = () => {
        $uibModalInstance.close();

        const uri = apiBaseURL + "delete-pkg";

        $http({
            method: 'PUT',
            url: uri,
            headers: {"Content-Type": "application/json"},
            data: linearId
        }).then(
            (result) => deletePkgModal.displayMessage(result),
            (result) => deletePkgModal.displayMessage(result)
        ).catch(function onError(error) {
           console.log(error);
        });
    };

    /* Displays the success/failure response from delete package operation */
    deletePkgModal.displayMessage = (message) => {
        const deletePkgMsgModal = $uibModal.open({
            templateUrl  : 'deletePkgMsgModal.html',
            controller   : 'deletePkgMsgModalCtrl',
            controllerAs : 'deletePkgMsgModal',
            resolve : { message : () => message }
        });

        /* No behaviour on close/dismiss */
        deletePkgMsgModal.result.then(() => {}, () => {});
    };

    /* Closes the buy pkg modal */
    deletePkgModal.cancel = () => $uibModalInstance.dismiss();
});

angular.module('devModule').controller('deletePkgMsgModalCtrl', function($uibModalInstance, message) {
    const deletePkgMsgModal = this;
    deletePkgMsgModal.message = message.data;
});