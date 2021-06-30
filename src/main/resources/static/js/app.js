// Define the app module
const app = angular.module('repositoryApp', []);

app.controller('MainController', function MainController($scope) {
    $scope.x = ["abc", "def"];
});