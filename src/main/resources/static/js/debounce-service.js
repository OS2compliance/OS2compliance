export function debounce(fn, delay = 1000, options = {}) {
    let timeoutId;
    let lastArgs = [];
    let lastThis;
    const { resetEvents = [], resetTarget = document } = options;
    const cleanupFns = [];

    const debouncedFn = function (...args) {
        lastArgs = args;
        lastThis = this;
        clearTimeout(timeoutId);
        timeoutId = setTimeout(() => fn.apply(lastThis, lastArgs), delay);
    };

    if (resetEvents.length > 0) {
        const resetHandler = () => {
            if (timeoutId) {
                clearTimeout(timeoutId);
                timeoutId = setTimeout(() => fn.apply(lastThis, lastArgs), delay);
            }
        };

        resetEvents.forEach(event => {
            resetTarget.addEventListener(event, resetHandler, { passive: true });
            cleanupFns.push(() => resetTarget.removeEventListener(event, resetHandler));
        });
    }

    debouncedFn.cancel = () => {
        clearTimeout(timeoutId);
        cleanupFns.forEach(cleanup => cleanup());
        cleanupFns.length = 0;
    };

    return debouncedFn;
}