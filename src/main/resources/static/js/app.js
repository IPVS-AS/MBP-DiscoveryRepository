// Application module
const app = angular.module('repositoryApp', ['smart-table']);

/**
 * Application configuration.
 */
app.config(['$provide', '$locationProvider',
    function ($provide, $locationProvider) {
        //Enable HTML5Mode to disable hashbang urls
        $locationProvider.html5Mode({
            enabled: true,
            requireBase: false
        });
    }]);

/**
 * Main controller.
 */
app.controller('MainController', ['$scope', '$http',
    function MainController($scope, $http) {

        /**
         * Initializes the controller.
         */
        (function () {
            $scope.deviceDescriptionsList = [];
            $http.get('/deviceDescriptions').then((response) => {
                    $scope.deviceDescriptionsList = response.data;
                },
                () => alert("Failed to retrieve device descriptions.")
            )
        })();

        /**
         * Adds a device description to the repository.
         */
        $scope.addDeviceDescription = function () {
            alert($scope.deviceDescriptionInput);
        }

        /**
         * Deletes a device description with a certain ID from the repository.
         * @param id The ID of the device description to delete
         */
        $scope.deleteDeviceDescription = function (id) {
            alert("Delete " + id);
        }
    }]);
