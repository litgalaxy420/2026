/**
 * Syringe - CSS Injection Scanner
 * A tool to find CSS files and check for inline styles vulnerable to CSS injection
 * 
 * Created by: Caleb D.
 * Color Scheme: Blue Raspberry
 */

const http = require('http');
const https = require('https');
const url = require('url');
const fs = require('fs');

// Blue Raspberry Color Scheme
const colors = {
    blue: '#00A8E8',
    darkBlue: '#007EA7',
    lightBlue: '#00C3FF',
    raspberry: '#C21E56',
    light: '#E8F4F8',
    white: '#FFFFFF',
    gray: '#888888'
};

function log(message, type = 'info') {
    const timestamp = new Date().toISOString();
    let color = colors.blue;
    
    switch(type) {
        case 'vuln': color = colors.raspberry; break;
        case 'found': color = colors.lightBlue; break;
        case 'success': color = colors.darkBlue; break;
        case 'error': color = colors.raspberry; break;
    }
    
    console.log(`\x1b[38;2;0;${color === colors.blue ? '168' : color === colors.raspberry ? '201' : color === colors.lightBlue ? '195' : color === colors.darkBlue ? '126' : '136'};232m${timestamp}\x1b[0m [${type.toUpperCase()}] ${message}`);
}

async function fetchPage(targetUrl) {
    return new Promise((resolve, reject) => {
        const protocol = targetUrl.startsWith('https') ? https : http;
        
        protocol.get(targetUrl, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                resolve(data);
            });
            
        }).on('error', (err) => {
            reject(err);
        });
    });
}

async function fetchCSS(cssUrl) {
    return new Promise((resolve, reject) => {
        const protocol = cssUrl.startsWith('https') ? https : http;
        
        protocol.get(cssUrl, (res) => {
            let data = '';
            
            res.on('data', (chunk) => {
                data += chunk;
            });
            
            res.on('end', () => {
                resolve(data);
            });
            
        }).on('error', (err) => {
            reject(err);
        });
    });
}

function extractCSSLinks(html, baseUrl) {
    const cssLinks = [];
    
    // External CSS links
    const linkRegex = /<link[^>]+href=["']([^"']+\.css[^"']*)["'][^>]*>/gi;
    let match;
    
    while ((match = linkRegex.exec(html)) !== null) {
        const href = match[1];
        const absoluteUrl = new URL(href, baseUrl).href;
        cssLinks.push(absoluteUrl);
    }
    
    return cssLinks;
}

function extractInlineStyles(html) {
    const inlineStyles = [];
    
    // Check for style attributes
    const styleAttrRegex = /<([a-zA-Z][a-zA-Z0-9]*)[^>]*style=["']([^"']+)["'][^>]*>/gi;
    let match;
    
    while ((match = styleAttrRegex.exec(html)) !== null) {
        inlineStyles.push({
            element: match[1],
            styles: match[2],
            line: html.substring(0, match.index).split('\n').length
        });
    }
    
    // Check for <style> tags
    const styleTagRegex = /<style[^>]*>([\s\S]*?)<\/style>/gi;
    
    while ((match = styleTagRegex.exec(html)) !== null) {
        inlineStyles.push({
            element: 'style tag',
            styles: match[1].trim(),
            line: html.substring(0, match.index).split('\n').length
        });
    }
    
    return inlineStyles;
}

function checkForCSSInjectionVulnerabilities(inlineStyles, cssContent, cssUrl) {
    const vulnerabilities = [];
    
    // Check for dynamic/expression-like patterns
    const dangerousPatterns = [
        { pattern: /expression\s*\(/gi, name: 'Expression Usage' },
        { pattern: /url\s*\([^)]*javascript:/gi, name: 'JavaScript in URL' },
        { pattern: /@import/gi, name: 'Import Directive' },
        { pattern: /behavior\s*:/gi, name: 'Behavior Property' },
        { pattern: /-moz-binding/gi, name: 'Moz Binding' },
        { pattern: /@charset/gi, name: 'Charset Directive' },
        { pattern: /src\s*=[^>]*data:/gi, name: 'Data URI' },
        { pattern: /attr\s*\([^)]*\)\s*[+]/gi, name: 'Attribute Concatenation' },
        { pattern: /calc\s*\(/gi, name: 'Calc Function' },
        { pattern: /var\s*\(/gi, name: 'CSS Variable Usage' }
    ];
    
    // Check inline styles for dangerous patterns
    inlineStyles.forEach(style => {
        dangerousPatterns.forEach(({pattern, name}) => {
            if (pattern.test(style.styles)) {
                vulnerabilities.push({
                    type: 'inline',
                    name: name,
                    content: style.styles.substring(0, 100),
                    element: style.element,
                    line: style.line,
                    severity: 'HIGH'
                });
            }
        });
    });
    
    // Check CSS content for dangerous patterns
    dangerousPatterns.forEach(({pattern, name}) => {
        if (pattern.test(cssContent)) {
            vulnerabilities.push({
                type: 'css_file',
                name: name,
                url: cssUrl,
                severity: 'MEDIUM'
            });
        }
    });
    
    return vulnerabilities;
}

function checkForDataExfiltrationVectors(cssContent) {
    const vectors = [];
    
    // Check for attribute selectors that could leak data
    const attrSelectors = /\[([^\]]+)\]/g;
    let match;
    
    while ((match = attrSelectors.exec(cssContent)) !== null) {
        if (match[1].includes('href') || match[1].includes('src') || match[1].includes('value')) {
            vectors.push({
                type: 'attribute_selector',
                pattern: match[0],
                description: 'Attribute selector could potentially leak attribute values'
            });
        }
    }
    
    return vectors;
}

async function scanWebsite(targetUrl) {
    console.log(`
    ╔════════════════════════════════════════════════════════════╗
    ║                                                            ║
    ║   ██████╗ ██████╗  █████╗ ██╗     ██╗     ███████╗██████╗  ║
    ║   ██╔══██╗██╔══██╗██╔══██╗██║     ██║     ██╔════╝██╔══██╗ ║
    ║   ██████╔╝██████╔╝███████║██║     ██║     █████╗  ██████╔╝ ║
    ║   ██╔═══╝ ██╔══██╗██╔══██║██║     ██║     ██╔══╝  ██╔══██╗ ║
    ║   ██║     ██║  ██║██║  ██║███████╗███████╗███████╗██║  ██║ ║
    ║   ╚═╝     ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝╚═╝  ╚═╝ ║
    ║                                                            ║
    ║              CSS Injection Vulnerability Scanner           ║
    ║                                                            ║
    ║                    Created by: Caleb D.                     ║
    ║                                                            ║
    ╚════════════════════════════════════════════════════════════╝
    `);
    
    log(`Starting scan of: ${targetUrl}`, 'info');
    
    try {
        // Fetch the main page
        log('Fetching main page...', 'info');
        const html = await fetchPage(targetUrl);
        
        // Extract CSS links
        log('Extracting CSS file links...', 'info');
        const cssLinks = extractCSSLinks(html, targetUrl);
        log(`Found ${cssLinks.length} CSS file(s)`, 'found');
        
        // Extract inline styles
        log('Analyzing inline styles...', 'info');
        const inlineStyles = extractInlineStyles(html);
        log(`Found ${inlineStyles.length} inline style(s)`, 'found');
        
        let allVulnerabilities = [];
        let allDataVectors = [];
        let scannedCSS = 0;
        
        // Scan inline styles first
        log('Scanning inline styles for vulnerabilities...', 'info');
        inlineStyles.forEach(style => {
            const vulns = checkForCSSInjectionVulnerabilities([style], '', '');
            if (vulns.length > 0) {
                allVulnerabilities = allVulnerabilities.concat(vulns);
                vulns.forEach(v => {
                    log(`Potential vulnerability found: ${v.name}`, 'vuln');
                    log(`  Element: ${v.element}, Line: ${v.line}`, 'vuln');
                });
            }
        });
        
        // Scan each CSS file
        for (const cssUrl of cssLinks) {
            scannedCSS++;
            log(`Scanning CSS file ${scannedCSS}/${cssLinks.length}: ${cssUrl}`, 'info');
            
            try {
                const cssContent = await fetchCSS(cssUrl);
                
                // Check for vulnerabilities
                const vulns = checkForCSSInjectionVulnerabilities([], cssContent, cssUrl);
                if (vulns.length > 0) {
                    allVulnerabilities = allVulnerabilities.concat(vulns);
                }
                
                // Check for data exfiltration vectors
                const vectors = checkForDataExfiltrationVectors(cssContent);
                allDataVectors = allDataVectors.concat(vectors);
                
            } catch (err) {
                log(`Failed to fetch CSS: ${cssUrl}`, 'error');
            }
        }
        
        // Generate report
        console.log(`
    ╔════════════════════════════════════════════════════════════╗
    ║                      SCAN RESULTS                          ║
    ╚════════════════════════════════════════════════════════════╝
    `);
        
        log(`Target: ${targetUrl}`, 'info');
        log(`CSS Files Found: ${cssLinks.length}`, 'info');
        log(`Inline Styles Found: ${inlineStyles.length}`, 'info');
        log(`CSS Files Scanned: ${scannedCSS}`, 'info');
        
        console.log('');
        
        if (allVulnerabilities.length === 0) {
            log('No obvious CSS injection vulnerabilities detected.', 'success');
            log('Note: This does not guarantee the site is fully secure.', 'info');
        } else {
            log(`Found ${allVulnerabilities.length} potential vulnerability/vulnerabilities:`, 'vuln');
            console.log('');
            
            allVulnerabilities.forEach((vuln, index) => {
                console.log(`  ${index + 1}. [${vuln.severity}] ${vuln.name}`);
                if (vuln.type === 'inline') {
                    console.log(`     Element: ${vuln.element}`);
                    console.log(`     Line: ${vuln.line}`);
                    console.log(`     Content: ${vuln.content.substring(0, 50)}...`);
                } else if (vuln.type === 'css_file') {
                    console.log(`     File: ${vuln.url}`);
                }
                console.log('');
            });
        }
        
        if (allDataVectors.length > 0) {
            log(`Found ${allDataVectors.length} data exfiltration vector(s):`, 'found');
            console.log('');
            
            allDataVectors.forEach((vector, index) => {
                console.log(`  ${index + 1}. [${vector.type.toUpperCase()}]`);
                console.log(`     Pattern: ${vector.pattern}`);
                console.log(`     Description: ${vector.description}`);
                console.log('');
            });
        }
        
        console.log(`
    ╔════════════════════════════════════════════════════════════╗
    ║                    SCAN COMPLETE                            ║
    ║                                                            ║
    ║              Syringe - CSS Injection Scanner               ║
    ║                    Created by: Caleb D.                    ║
    ╚════════════════════════════════════════════════════════════╝
    `);
        
        return {
            success: true,
            vulnerabilities: allVulnerabilities,
            dataVectors: allDataVectors,
            stats: {
                cssFilesFound: cssLinks.length,
                inlineStylesFound: inlineStyles.length,
                cssFilesScanned: scannedCSS
            }
        };
        
    } catch (err) {
        log(`Error scanning website: ${err.message}`, 'error');
        return {
            success: false,
            error: err.message
        };
    }
}

// CLI Interface
function showHelp() {
    console.log(`
    Syringe - CSS Injection Vulnerability Scanner
    
    Usage: node syringe.js [options] <url>
    
    Options:
    -h, --help     Show this help message
    -o, --output   Save results to JSON file
    -v, --verbose  Enable verbose output
    
    Examples:
    node syringe.js https://example.com
    node syringe.js -o results.json https://example.com
    node syringe.js -v https://example.com
    
    Created by: Caleb D.
    Color Scheme: Blue Raspberry
    `);
}

// Main execution
const args = process.argv.slice(2);

if (args.length === 0 || args[0] === '-h' || args[0] === '--help') {
    showHelp();
    process.exit(0);
}

const verbose = args.includes('-v') || args.includes('--verbose');
const outputIndex = args.indexOf('-o') !== -1 ? args.indexOf('-o') : args.indexOf('--output');
const helpIndex = args.indexOf('-h') !== -1 || args.indexOf('--help') !== -1;

let targetUrl = '';

// Extract URL from arguments
for (const arg of args) {
    if (!arg.startsWith('-') && arg !== 'node' && arg !== 'syringe.js') {
        targetUrl = arg;
        break;
    }
}

if (!targetUrl) {
    console.log('Error: Please provide a URL to scan');
    console.log('Use --help for usage information');
    process.exit(1);
}

// Validate URL
try {
    new URL(targetUrl);
} catch (err) {
    console.log('Error: Invalid URL provided');
    console.log('Use --help for usage information');
    process.exit(1);
}

// Run scan
scanWebsite(targetUrl).then(result => {
    // Save results if output flag is set
    if (outputIndex !== -1 && args[outputIndex + 1]) {
        const outputFile = args[outputIndex + 1];
        fs.writeFileSync(outputFile, JSON.stringify(result, null, 2));
        log(`Results saved to: ${outputFile}`, 'success');
    }
    
    process.exit(result.success ? 0 : 1);
}).catch(err => {
    console.error('Fatal error:', err);
    process.exit(1);
});

module.exports = { scanWebsite, extractCSSLinks, extractInlineStyles, checkForCSSInjectionVulnerabilities };
