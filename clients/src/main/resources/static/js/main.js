"use strict";

angular.module('mainModule', ['ui.bootstrap']).controller('MainCtrl', function($http, $location, $uibModal, $window) {
    const main = this;
    const apiBaseURL = "/";

    $http.get(apiBaseURL + "me").then((response) => {
        main.thisNode = response.data.me;
        if(main.thisNode == "O=Buyer, L=Pisa, C=IT")
            document.getElementsByTagName("link").item(2).href = "./css/buyer.css";
        else if(main.thisNode == "O=RepositoryNode, L=Pisa, C=IT")
            document.getElementsByTagName("link").item(2).href = "./css/repositoryNode.css";
    });

    /* Displays the self cash issuance modal */
    main.openSelfIssueCashModal = () => {
        const selfIssueCashModal = $uibModal.open({
            templateUrl  : 'selfIssueCashModal.html',
            controller   : 'SelfIssueCashModalCtrl',
            controllerAs : 'selfIssueCashModal',
            resolve      : { apiBaseURL : () => apiBaseURL }
        });

        /* Ignores the modal result events */
        selfIssueCashModal.result.then(() => {}, () => {});
    };

    /* Displays the Details of a package selected in the marketplace */
    main.openPkgDetailsModal = (pkg) => {
        const pkgDetailsModal = $uibModal.open({
            templateUrl  : 'pkgDetailsModal.html',
            controller   : 'PkgDetailsModalCtrl',
            controllerAs : 'pkgDetailsModal',
            resolve      : { pkg: () => pkg }
        });

        /* Ignores the modal result events */
        pkgDetailsModal.result.then(() => {}, () => {});
    };

    /* Displays the Buy package modal for the package selected in the marketplace */
    main.openBuyPkgModal = (linearId, pkgInfoId, pkgType, price) => {
        const buyPkgModal = $uibModal.open({
            templateUrl  : 'buyPkgModal.html',
            controller   : 'BuyPkgModalCtrl',
            controllerAs : 'buyPkgModal',
            resolve      : {
                apiBaseURL : () => apiBaseURL,
                linearId: () => linearId,
                pkgInfoId: () => pkgInfoId,
                pkgType: () => pkgType,
                price: () => price
            }
        });

        /* Ignores the modal result events */
        buyPkgModal.result.then(() => {}, () => {});
    };

    main.refresh = () => {
        /* Update the marketplace (list of pkgs) */
        $http.get(apiBaseURL + "marketplace").then((response) => main.pkgs = response.data)
        .catch(function onError(error) {
            console.log(error);
        });

        /* Update the licenses of the user */
        $http.get(apiBaseURL + "pkg-license-state").then((response) => main.licenses = response.data)
        .catch(function onError(error) {
            console.log(error);
        });

        /* Update the cash balances */
        $http.get(apiBaseURL + "cash-balances").then((response) => main.cashBalances = response.data)
        .catch(function onError(error) {
            console.log(error);
        });
    }

    main.devPage = () => { $window.location.href = 'dev.html'; }

    main.refresh();
});

/* Causes the webapp to ignore unhandled modal dismissals */
angular.module('mainModule').config(['$qProvider', function($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);