"use strict";

angular.module('devModule', ['ui.bootstrap']).controller('DevCtrl', function($http, $location, $uibModal, $window) {
    const dev = this;
    const apiBaseURL = "/";

    $http.get(apiBaseURL + "me").then((response) => dev.thisNode = response.data.me);

    /* Displays the fee agreement establishment modal */
    dev.openFeeAgreementModal = () => {
        const feeAgreementModal = $uibModal.open({
            templateUrl  : 'feeAgreementModal.html',
            controller   : 'FeeAgreementModalCtrl',
            controllerAs : 'feeAgreementModal',
            resolve      : { apiBaseURL : () => apiBaseURL }
        });

        /* Ignores the modal result events */
        feeAgreementModal.result.then(() => {}, () => {});
    };

    /* Displays the pkg register modal */
    dev.openRegisterPkgModal = () => {
        const registerPkgModal = $uibModal.open({
            templateUrl  : 'registerPkgModal.html',
            controller   : 'RegisterPkgModalCtrl',
            controllerAs : 'registerPkgModal',
            resolve      : { apiBaseURL : () => apiBaseURL }
        });

        /* Ignores the modal result events */
        registerPkgModal.result.then(() => {}, () => {});
    };

    /* Displays the Details of a dev's package */
    dev.openPkgDetailsModal = (pkg) => {
        const pkgDetailsModal = $uibModal.open({
            templateUrl  : 'pkgDetailsModal.html',
            controller   : 'PkgDetailsModalCtrl',
            controllerAs : 'pkgDetailsModal',
            resolve      : { pkg: () => pkg }
        });

        /* Ignores the modal result events */
        pkgDetailsModal.result.then(() => {}, () => {});
    };

    /* Displays the pkg update modal */
    dev.openUpdatePkgModal = () => {
        const updatePkgModal = $uibModal.open({
            templateUrl  : 'updatePkgModal.html',
            controller   : 'UpdatePkgModalCtrl',
            controllerAs : 'updatePkgModal',
            resolve      : { apiBaseURL : () => apiBaseURL }
        });

        /* Ignores the modal result events */
        updatePkgModal.result.then(() => {}, () => {});
    };

    /* Displays the pkg delete modal */
    dev.openDeletePkgModal = (linearId) => {
        const deletePkgModal = $uibModal.open({
            templateUrl  : 'deletePkgModal.html',
            controller   : 'DeletePkgModalCtrl',
            controllerAs : 'deletePkgModal',
            resolve      : {
                apiBaseURL : () => apiBaseURL,
                linearId   : () => linearId
            }
        });

        /* Ignores the modal result events */
        deletePkgModal.result.then(() => {}, () => {});
    };

    dev.refresh = () => {
        /* Update the list of the developer's packages */
        $http.get(apiBaseURL + "pkg-offer-state").then((response) => dev.pkgs = response.data)
        .catch(function onError(error) {
           console.log(error);
        });
    }

    dev.indexPage = () => { $window.location.href = 'index.html'; }

    dev.refresh();
});