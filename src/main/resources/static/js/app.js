// Application module
const app = angular.module('repositoryApp', ['smart-table', 'angular-loading-bar']);

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

        //Configuration
        const ALERT_TIMEOUT = 2000;

        /**
         * Initializes the controller.
         */
        (function () {
            //Get device descriptions
            $scope.deviceDescriptionsList = [];
            $http.get('/deviceDescriptions').then((response) => {
                $scope.deviceDescriptionsList = response.data;
            }, handleRequestFailure);

            //Get example device description
            $scope.exampleDeviceDescription = "";
            $http.get('/example').then((response) => {
                $scope.exampleDeviceDescription = JSON.stringify(response.data, null, 4);
                $scope.deviceDescriptionInput = $scope.exampleDeviceDescription;
            }, handleRequestFailure);

            //Get status
            $scope.loadingStatus = true;
            $scope.status = {};
            $http.get('/status').then((response) => {
                $scope.status = response.data;
            }, handleRequestFailure).finally(() => {
                $scope.loadingStatus = false;
            });
        })();

        /**
         * Performs a server request in order to insert a new device description into the repository.
         */
        $scope.addDeviceDescription = function () {
            //Clear device description validation errors
            $scope.deviceDescriptionErrors = [];

            //Perform POST request
            $http.post("/deviceDescriptions", $scope.deviceDescriptionInput).then(function (response) {
                //Add inserted device description to list
                $scope.deviceDescriptionsList.push(response.data);

                //Show success alert
                Swal.fire({
                    title: 'Success',
                    text: 'The device description was successfully inserted into the repository.',
                    icon: 'success',
                    timer: ALERT_TIMEOUT
                });
            }, (response) => handleRequestFailure(response, function (detailMessages) {
                //Set device description validation errors if available
                $scope.deviceDescriptionErrors = detailMessages;
            }));
        }

        /**
         * Asks the users whether they are sure that they want to delete a certain device description. If they agree,
         * a server request will be performed in order to delete the device description that matches the given
         * identifier from the repository.
         *
         * @param id The identifier of the device description to delete
         */
        $scope.deleteDeviceDescription = function (id) {
            /**
             * Performs a server request in order to delete the device description that matches the given identifier
             * from the repository.
             * @param id The identifier of the device description to delete
             */
            function performDeleteRequest(id) {
                //Perform DELETE request
                $http.delete("/deviceDescriptions/" + id).then(() => {
                    //Find index of deleted device description in list
                    let index = $scope.deviceDescriptionsList.map(x => {
                        return x.id;
                    }).indexOf(id);
                    //Remove device description from list
                    $scope.deviceDescriptionsList.splice(index, 1);
                    //Show success alert
                    Swal.fire({
                        title: 'Success',
                        text: 'The device description was successfully deleted.',
                        icon: 'success',
                        timer: ALERT_TIMEOUT
                    });
                }, handleRequestFailure);
            }

            //Get index and name of the device description
            let index = $scope.deviceDescriptionsList.map(x => {
                return x.id;
            }).indexOf(id);
            let descriptionName = $scope.deviceDescriptionsList[index].name;

            //Show prompt to the users asking whether they are sure
            Swal.fire({
                title: 'Delete device description',
                html: 'Are you sure you want to delete the device description "<strong>' + descriptionName + '</strong>"?',
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#3085d6',
                cancelButtonColor: '#d33',
                confirmButtonText: 'Delete'
            }).then((result) => {
                //Check whether user confirmed
                if (result.isConfirmed) {
                    //Perform deletion request
                    performDeleteRequest(id);
                }
            })
        }

        /**
         * [Private]
         * Handles failed HTTP requests by evaluating the server response. If the server response contains detail
         * messages about the occurred error, these can be optionally passed to a given callback handler function.
         * @param response The server response of the failed request
         * @param handleDetailMessages Callback function in order to handle detail messages (if available)
         */
        function handleRequestFailure(response, handleDetailMessages) {
            //Check if data is available
            if ((!response.hasOwnProperty("data")) || (response.data == null)) {
                Swal.fire('Error', 'The request failed for unknown reason. Is the application online?', 'error');
                return;
            }

            //Data is available, so check if an error message is available
            if (response.data.hasOwnProperty("message") && (response.data.message != null) && (response.data.message.length > 1)) {
                Swal.fire('Error', response.data.message, 'error');
            } else {
                Swal.fire('Error', "The request failed.", 'error');
            }

            //Check if detail messages are available and a callback is set
            if (response.data.hasOwnProperty("detailMessages") && (response.data.detailMessages != null) && (handleDetailMessages instanceof Function)) {
                //Call callback with detail messages
                handleDetailMessages(response.data.detailMessages);
            }
        }
    }]);
