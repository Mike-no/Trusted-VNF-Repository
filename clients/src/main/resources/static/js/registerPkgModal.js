"use strict";

angular.module('devModule').controller('RegisterPkgModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL) {
    const registerPkgModal = this;

    registerPkgModal.registerPkg = () => {
        $uibModalInstance.close();

        var pkg = null;
        try {
            pkg = JSON.parse(document.getElementById('registerJson').value);
            const uri = apiBaseURL + "register-pkg";

            $http({
                method: 'PUT',
                url: uri,
                headers: {"Content-Type": "application/json"},
                data: pkg
            }).then(
                (result) => registerPkgModal.displayMessage(result.data),
                (result) => registerPkgModal.displayMessage(result.data)
            ).catch(function onError(error) {
                 console.log(error);
            });
        } catch(error) {
            if(error instanceof SyntaxError)
                registerPkgModal.displayMessage("There was a syntax error. Please correct it and try again: " + error.message);
            else
                throw error;
        }
    };

    /* Displays the success/failure response from register package operation */
    registerPkgModal.displayMessage = (message) => {
        const registerPkgMsgModal = $uibModal.open({
            templateUrl  : 'registerPkgMsgModal.html',
            controller   : 'registerPkgMsgModalCtrl',
            controllerAs : 'registerPkgMsgModal',
            resolve : { message : () => message }
        });

        /* No behaviour on close/dismiss */
        registerPkgMsgModal.result.then(() => {}, () => {});
    };

    /* Closes the register pkg modal */
    registerPkgModal.cancel = () => $uibModalInstance.dismiss();
});

angular.module('devModule').controller('registerPkgMsgModalCtrl', function($uibModalInstance, message) {
    const registerPkgMsgModal = this;
    registerPkgMsgModal.message = message;
});