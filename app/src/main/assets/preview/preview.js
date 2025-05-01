const preEl = document.querySelector('pre')
const darkStyle = document.getElementById('code-dark')
const lightStyle = document.getElementById('code-light')

const observer = new MutationObserver(mutationObserver)
observer.observe(preEl, { childList: true, subtree: true })

document.addEventListener('DOMContentLoaded', () => {
    console.log('DOMContentLoaded')
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    applyTheme(mediaQuery)
    mediaQuery.addEventListener('change', applyTheme)
})

function mutationObserver(mutationsList) {
    for (const mutation of mutationsList) {
        if (mutation.type === 'childList') {
            console.log('highlightElement')
            hljs.highlightElement(preEl)
        }
    }
}

function applyTheme(mediaQuery) {
    console.log(`applyTheme: matches: ${mediaQuery.matches}`)
    if (mediaQuery.matches) {
        darkStyle.disabled = false
        lightStyle.disabled = true
    } else {
        darkStyle.disabled = true
        lightStyle.disabled = false
    }
}
