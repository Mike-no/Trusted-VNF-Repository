"use strict";

angular.module('devModule').controller('PkgDetailsModalCtrl', function($uibModalInstance, $uibModal, pkg) {
    const pkgDetailsModal = this;
    pkgDetailsModal.pkg = JSON.stringify(pkg, undefined, 2);

    pkgDetailsModal.cancel = () => $uibModalInstance.dismiss();
});
