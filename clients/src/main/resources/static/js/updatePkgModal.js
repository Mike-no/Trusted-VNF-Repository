"use strict";

angular.module('devModule').controller('UpdatePkgModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const updatePkgModal = this;

    updatePkgModal.updatePkg = () => {
        $uibModalInstance.close();

        var update = null;
        try {
            update = JSON.parse(document.getElementById('updateJson').value);
            const uri = apiBaseURL + "update-pkg";

            $http({
                method: 'PUT',
                url: uri,
                headers: {"Content-Type": "application/json"},
                data: update
            }).then(
                (result) => updatePkgModal.displayMessage(result.data),
                (result) => updatePkgModal.displayMessage(result.data)
            ).catch(function onError(error) {
                 console.log(error);
            });
        } catch(error) {
          if(error instanceof SyntaxError)
              updatePkgModal.displayMessage("There was a syntax error. Please correct it and try again: " + error.message);
          else
              throw error;
      }
    };

    /* Displays the success/failure response from update package operation */
    updatePkgModal.displayMessage = (message) => {
        const updatePkgMsgModal = $uibModal.open({
            templateUrl  : 'updatePkgMsgModal.html',
            controller   : 'updatePkgMsgModalCtrl',
            controllerAs : 'updatePkgMsgModal',
            resolve : { message : () => message }
        });

        /* No behaviour on close/dismiss */
        updatePkgMsgModal.result.then(() => {}, () => {});
    };

    /* Closes the update pkg modal */
    updatePkgModal.cancel = () => $uibModalInstance.dismiss();
});

angular.module('devModule').controller('updatePkgMsgModalCtrl', function($uibModalInstance, message) {
    const updatePkgMsgModal = this;
    updatePkgMsgModal.message = message;
});