// MicroModal.init();
$(function() {
    console.log("przed init")
    MicroModal.init({
        onShow: modal => console.info(`${modal.id} is shown`), // [1]
        onClose: modal => console.info(`${modal.id} is hidden`), // [2]
        // openTrigger: 'data-custom-open', // [3]
        // closeTrigger: 'data-custom-close', // [4]
        // openClass: 'dialog-open', // [5]
        disableScroll: true, // [6]
        disableFocus: false, // [7]
        awaitOpenAnimation: false, // [8]
        awaitCloseAnimation: false, // [9]
        debugMode: true // [10]
    });
    console.log("po init")
    // MicroModal.show('modal-1');
    console.log("co jest kurwa")
})

