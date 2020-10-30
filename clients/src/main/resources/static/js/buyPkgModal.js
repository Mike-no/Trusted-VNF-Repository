"use strict";

angular.module('mainModule').controller('BuyPkgModalCtrl', function($http, $uibModalInstance, $uibModal, apiBaseURL,
    linearId, pkgInfoId, pkgType, price) {
    const buyPkgModal = this;

    buyPkgModal.buyPkg = () => {
        $uibModalInstance.close();

        /* Define the buy pkg endpoint */
        const data = {
            "linearId"  : linearId,
            "pkgInfoId" : pkgInfoId,
            "pkgType"   : pkgType,
            "price"     : price
        };
        const uri = apiBaseURL + "marketplace/buy-pkg";

        $http({
            method: 'POST',
            url: uri,
            headers: {"Content-Type": "application/json"},
            data: data
        }).then(
            (result) => buyPkgModal.displayMessage(result),
            (result) => buyPkgModal.displayMessage(result)
        );
    };

    /* Displays the success/failure response from buy package operation */
    buyPkgModal.displayMessage = (message) => {
        const buyPkgMsgModal = $uibModal.open({
            templateUrl  : 'buyPkgMsgModal.html',
            controller   : 'buyPkgMsgModalCtrl',
            controllerAs : 'buyPkgMsgModal',
            resolve : { message : () => message }
        });

        /* No behaviour on close/dismiss */
        buyPkgMsgModal.result.then(() => {}, () => {});
    };

    /* Closes the buy pkg modal */
    buyPkgModal.cancel = () => $uibModalInstance.dismiss();
});

angular.module('mainModule').controller('buyPkgMsgModalCtrl', function($uibModalInstance, message) {
    const buyPkgMsgModal = this;
    buyPkgMsgModal.message = message.data;
});