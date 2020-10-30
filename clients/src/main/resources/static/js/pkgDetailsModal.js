"use strict";

angular.module('mainModule').controller('PkgDetailsModalCtrl', function($uibModalInstance, $uibModal, pkg) {
    const pkgDetailsModal = this;
    pkgDetailsModal.pkg = pkg;

    pkgDetailsModal.cancel = () => $uibModalInstance.dismiss();
});
