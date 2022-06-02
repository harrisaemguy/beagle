window.dexter = window.dexter || {}; window.dexter.utils = window.dexter.utils || {}; if (!window.IntersectionObserver) { document.dispatchEvent(new Event('dexter:headPolyfillLoaded')); window.dexter.utils.headPolyfill = true; } function setTheme() { // Set default theme document.documentElement.setAttribute('theme', 'system') // Set theme per query param; will override default var themeQuery = window.location.search .slice(1) .split('&') .find(function (q) { return q.indexOf('theme=') !== -1 }) if (themeQuery && themeQuery.split('=').length > 1) { var theme = themeQuery.split('=')\[1\] if (\['light', 'dark'\].includes(theme)) { document.documentElement.setAttribute('theme', theme) } } } setTheme() Captivate Prime Get Started                                                                                        if (typeof (AdobeLearn) === "undefined") AdobeLearn = {}; if (typeof (AdobeLearn.Locale) === "undefined") AdobeLearn.Locale = {}; if (typeof (AdobeLearn.Config) === "undefined") AdobeLearn.Config = {}; AdobeLearn.Locale\["pageLocale"\] = 'en\_us'; AdobeLearn.Locale\["pageLanguage"\] = 'en'; AdobeLearn.Locale\["pageCountry"\] = 'US'; AdobeLearn.Locale\["nonEmptyUrlLocale"\] = 'en'; AdobeLearn.Locale\["urlLocale"\] = ''; AdobeLearn.Locale\["folderLocale"\] = 'en'; AdobeLearn.Locale\["contentUrl"\] = '\\/'; AdobeLearn.Locale\["contentFolder"\] = '\\/content\\/help\\/en\\/'; AdobeLearn.Locale\["datetimeLocale"\] = 'en'; AdobeLearn.Locale\["datetimeFormat"\] = 'MMM. dd, yyyy'; AdobeLearn.Locale\["parentLocale"\] = ''; AdobeLearn.Locale\["fiveLetterLocale"\] = 'en\_US'; AdobeLearn.Locale\["globalNavLocale"\] = 'en\_us'; AdobeLearn.Locale\["customPageLocale"\] = 'en\_US'; AdobeLearn.Locale\["customPageLanguage"\] = 'en'; AdobeLearn.Config\["tier"\] = 'PROD'; AdobeLearn.Config\["serverType"\] = 'external'; AdobeLearn.Config\["externalLoadBalancer"\] = 'https:\\/\\/helpx.adobe.com'; AdobeLearn.Config\["author"\] = 'false'; AdobeLearn.Config\["author-preview"\] = 'false'; AdobeLearn.Config\["authorPrefix"\] = '/content/help/en'; AdobeLearn.Locale\["relativePath"\] = '/'; AdobeLearn.Config\["isHelpx"\] = 'true'; AdobeLearn.Config\["pageTemplate"\] = 'help/components/pages/getting-started'; AdobeLearn.Locale\["bcpCode"\] = 'en-us';                                                         body.locale\_zh\_CN div.article-footer div#askthecommunity { visibility:hidden; } body{display:none !important;} var pageIsIframed = window.self !== window.top; var isPartnerDomain = false; var partnerDomains = \[ new RegExp('https:\\/\\/.\*\\.adobe\\.com'), // adobe.com (this one had a problem http://regexr.com/3gfmk) new RegExp('https:\\/\\/adobe\\.com'), // all adobe subdomains new RegExp('https:\\/\\/.\*\\.adobecc\\.com'), // adobecc.com new RegExp('https:\\/\\/adobecc\\.com'), // new RegExp('https:\\/\\/echosign\\.com'), // echosign.com new RegExp('https:\\/\\/.\*\\.echosign\\.com'), // subdomains.echosign.com new RegExp('https:\\/\\/.\*\\.echocdn\\.com'), // subdomains.echocdn.com (MWPW-30824) new RegExp('https:\\/\\/.\*\\.echocdnstage\\.com'), // subdomains.echocdnstage.com (MWPW-30824) new RegExp('https:\\/\\/.\*\\.echocdnawspreview\\.com'), // subdomains.echocdnawspreview.com (MWPW-30824) new RegExp('https:\\/\\/.\*\\.echosignawspreview\\.com'), // subdomains.echosignawspreview.com new RegExp('https:\\/\\/.\*\\.echosignstage\\.com'), // subdomains.echosignstage.com new RegExp('https:\\/\\/.\*\\.echosigndemo\\.com'), // subdomains.echosigndemo.com new RegExp(':\\/\\/adobe\\.lookbookhq\\.com') // MWPW-24834 \]; // When this page is first is loaded in an iframe, the document.referrer == the parent url // which contains the iframe el (should be a partner domain). As the user navigates within that // iframe, the document.referrer will be from iframe's own domain (should also match a partner domain). var parentDomain = document.createElement("a"); parentDomain.href = document.referrer; parentDomain = parentDomain.protocol + '//' + parentDomain.hostname; partnerDomains.forEach(function(partnerDomain) { if(parentDomain.match(partnerDomain)){ isPartnerDomain = true; } }); if (!pageIsIframed || isPartnerDomain) { var d = document.getElementById('antiClickjack'); d.parentNode.removeChild(d); } if (pageIsIframed && parentDomain.indexOf('force') !== -1) { // Is a salesforce page which holds this iframe // hide feedback pod var feedbackPod = document.getElementById('feedbackPod'); if (typeof(feedbackPod) != 'undefined' && feedbackPod != null) { feedbackPod.style.display = "none"; } } if (typeof DEBUG == "undefined" ) { var DEBUG = (location.href.indexOf("debug=true")) > 0 ? true : false; }      var product = ""; var gnavExp = 'acom/ppbu-digital-mega-menu/captivate-prime-localnav'; var disableSearchTemplate = 'helpx/components/structure/helpxMain'; if(URLSearchParams){ var searchParams = new URLSearchParams(window.location.href); gnavExp = searchParams.get('gnavExp') || gnavExp; } window.fedsConfig = { locale: 'en', disableSticky: true, content: { experience: gnavExp, }, subnav: {}, footer: { regionModal: function () { window.location.hash = 'languageNavigation'; } }, breadcrumbs: { showLogo: true, links: \[\] }, privacy: { otDomainId: '7a5eb705\\u002D95ed\\u002D4cc4\\u002Da11d\\u002D0cc5760e93db' || '7a5eb705-95ed-4cc4-a11d-0cc5760e93db-test', footerLinkSelector: '\[data\\u002Dfeds\\u002Daction=\\x22open\\u002Dadchoices\\u002Dmodal\\x22\]' }, search: { context: '', }, disableSearch: 'help/components/pages/getting-started' === disableSearchTemplate }; window.dexter = window.dexter || {}; window.dexter.jarvis = { surfaceName: 'helpx-default', surfaceVersion: '1.0', onReady: function (newChatEnabled, jarvisData) { if (newChatEnabled) { if (typeof (enableLE) == 'function') { enableLE() }; } else { if (typeof (enableLP) == 'function') { enableLP() }; } }, onError: function () { if (typeof (enableLP) == 'function') { enableLP() }; }, openExistingChat: function () { if (typeof (enableLP) == 'function') { enableLP() }; }, getContext: (window.dexter && window.dexter.callbacks) ? window.dexter.callbacks.getContext : null } window.fedsConfig = window.fedsConfig || {}; window.fedsConfig.jarvis = { surfaceName: 'helpx-default', surfaceVersion: '1.0', onReady: function (newChatEnabled, jarvisData) { // Works for older templates // Disabled for new templates if (typeof (enableLE) == 'function') { enableLE(); } }, onError: function () { // Works for older templates // Disabled for new templates if (typeof (enableLP) == 'function') { enableLP(); } }, openExistingChat: function () { // Works for older templates // Disabled for new templates if (typeof (enableLP) == 'function') { enableLP(); } }, getContext: (window.dexter && window.dexter.callbacks) ? window.dexter.callbacks.getContext : null, directConfig: { lazyLoad: true } }

 [_![Captivate Prime](/content/dam/help/mnemonics/cp_prime_appicon_RGB.svg "Captivate Prime")_Captivate Prime](https://www.adobe.com/products/captivateprime.html)

*   [Learn & Support](/support/captivate-prime.html "Learn & Support")
*   [Get Started](/captivate-prime/get-started.html "Get Started")
*   [User Guide](/captivate-prime/user-guide.html "User Guide")

Captivate Prime Get Started
===========================

Search

var usseEndpoint = 'https://adobesearch.adobe.io/autocomplete/completions'; var usseApiKey = 'helpxcomprod'; var usseRedirectUrl = '/search.html'; var usseAutocompleteLocales = 'en,fr,de,ja';

Follow the steps below to get up and running with Captivate Prime

Start using Captivate Prime
---------------------------

[

![Captivate_prim_learner](/content/dam/help/en/captivate-prime/get-started/jcr%3acontent/main-pars/step_with_card/step-with-card-pars/tutorial_cards/tutorial-card-1/Captivate_prim_learner.png "Captivate_prim_learner")

Get started as a Learner

![](/content/dam/help/icons/getting-started/article_icon.svg) Article





](/captivate-prime/learners/feature-summary/getting-started.html)

[

![Captivate_prime_manager](/content/dam/help/en/captivate-prime/get-started/jcr%3acontent/main-pars/step_with_card/step-with-card-pars/tutorial_cards/tutorial-card-2/Captivate_prime_manager.png "Captivate_prime_manager")

Get started as a Manager

![](/content/dam/help/icons/getting-started/article_icon.svg) Article





](/captivate-prime/managers/feature-summary/getting-started.html)

[

![Captivate_prime_instructor](/content/dam/help/en/captivate-prime/get-started/jcr%3acontent/main-pars/step_with_card/step-with-card-pars/tutorial_cards/tutorial-card-3/Captivate_prime_instructor.png "Captivate_prime_instructor")

Get started as an Instructor

![](/content/dam/help/icons/getting-started/article_icon.svg) Article





](/captivate-prime/instructors/feature-summary/getting-started.html)

[

![Captivate_prime_author](/content/dam/help/en/captivate-prime/get-started/jcr%3acontent/main-pars/step_with_card/step-with-card-pars/tutorial_cards/tutorial-card-4/Captivate_prime_author.png "Captivate_prime_author")

Get started as an Author

![](/content/dam/help/icons/getting-started/article_icon.svg) Article





](/captivate-prime/authors/feature-summary/getting-started.html)

Help with your membership
-------------------------

### Forgot your Adobe ID or password?

Your Adobe ID is the email address you used when you first started a trial or purchased an Adobe app or membership. [Find solutions to common Adobe ID and sign-in issues](/manage-account/kb/account-password-sign-help.html). 

### Want to get a trial of Captivate Prime?

To get a trial of Captivate Prime, create your Adobe ID. For more information, see [Manage your Adobe ID](/manage-account.html). You can Sign in to Captivate Prime using your Adobe id and start your trial.

### Want to know more about managing the billing of your Captivate Prime account?

Administrators can manage the billing of accounts. For more information, see [Billing Management](/captivate-prime/administrators/feature-summary/billing-management.html).

**Best practices to set up Captivate Prime**

Refer to [best practices](/captivate-prime/administrators/getting-started.html).

**System Requirements**

Refer to [system requirements](/captivate-prime/system-requirements.html).

Helpful links

*   [Captivate Prime community forum](https://community.adobe.com/t5/captivate-prime/bd-p/captivate-prime?page=1&sort=latest_replies&filter=all)
*   [Captivate Prime community portal](https://elearning.adobe.com/)

var pathToMoreHelp = ""; var displayAemVersionString = "false"; var communityLink = "https:\\/\\/community.adobe.com\\/t5\\/captivate\\u002Dprime\\/ct\\u002Dp\\/ct\\u002Dcaptivate\\u002Dprime"; var contactLink = "\\/contact\\/enterprise\\u002Dsupport.other.html#captivate\\u002Dprime";

![Captivate Prime](/content/dam/help/mnemonics/cp_prime_appicon_RGB.svg)

Captivate Prime
===============

*   [< See all apps](/support.html#/all_products)

*   [Learn & Support](/support/captivate-prime.html)
*   Get Started
*   [User Guide](/captivate-prime/user-guide.html)

### Ask the Community

Post questions and get answers from experts.

[Ask now](https://community.adobe.com/t5/captivate-prime/ct-p/ct-captivate-prime)

### Contact Us

Real help from real people.

[Start now](/contact/enterprise-support.other.html#captivate-prime)

[^ Back to top](#)

Was this page helpful?

[Yes](javascript:showForm('yes');)

[No](javascript:showForm('no');)

No comment Submit

By clicking Submit, you accept the [Adobe Terms of Use.](https://www.adobe.com/legal/terms.html)

productIcon = "/content/dam/help/mnemonics/cp\_prime\_appicon\_RGB.svg";

###### Language Navigation

Language Navigation

[](#)

Choose a region

Selecting a region changes the language and/or content on Adobe.com.

*   Americas
    
*   [Brasil](https://helpx.adobe.com/content/help/br/pt/captivate-prime/get-started.html)
*   [Canada - English](https://helpx.adobe.com/content/help/ca/en/captivate-prime/get-started.html)
*   [Canada - Français](https://helpx.adobe.com/content/help/ca/fr/captivate-prime/get-started.html)
*   [Latinoamérica](https://helpx.adobe.com/content/help/la/es/captivate-prime/get-started.html)
*   [México](https://helpx.adobe.com/content/help/mx/es/captivate-prime/get-started.html)
*   [Chile](https://helpx.adobe.com/content/help/cl/es/captivate-prime/get-started.html)
*   [United States](https://helpx.adobe.com/content/help/en/captivate-prime/get-started.html)
*   Asia Pacific
    
*   [Australia](https://helpx.adobe.com/content/help/au/en/captivate-prime/get-started.html)
*   [Hong Kong S.A.R. of China](https://helpx.adobe.com/content/help/hk/en/captivate-prime/get-started.html)
*   [India - English](https://helpx.adobe.com/content/help/in/en/captivate-prime/get-started.html)
*   [New Zealand](https://helpx.adobe.com/content/help/nz/en/captivate-prime/get-started.html)
*   [Southeast Asia (Includes Indonesia, Malaysia, Philippines, Singapore, Thailand, and Vietnam) - English](https://helpx.adobe.com/content/help/sea/en/captivate-prime/get-started.html)
*   [中国](https://helpx.adobe.com/content/help/cn/zh-Hans/captivate-prime/get-started.html)
*   [中國香港特別行政區](https://helpx.adobe.com/content/help/hk/zh-Hant/captivate-prime/get-started.html)
*   [台灣地區](https://helpx.adobe.com/content/help/tw/zh-Hant/captivate-prime/get-started.html)
*   [日本](https://helpx.adobe.com/content/help/jp/ja/captivate-prime/get-started.html)
*   [한국](https://helpx.adobe.com/content/help/kr/ko/captivate-prime/get-started.html)
*   [Singapore](https://helpx.adobe.com/content/help/sg/en/captivate-prime/get-started.html)
*   [Thailand - English](https://helpx.adobe.com/content/help/th/en/captivate-prime/get-started.html)
*   [ประเทศไทย](https://helpx.adobe.com/content/help/th/th/captivate-prime/get-started.html)
*   Europe, Middle East and Africa
    
*   [Africa - English](https://helpx.adobe.com/content/help/africa/en/captivate-prime/get-started.html)
*   [België - Nederlands](https://helpx.adobe.com/content/help/be/nl/captivate-prime/get-started.html)
*   [Belgique - Français](https://helpx.adobe.com/content/help/be/fr/captivate-prime/get-started.html)
*   [Belgium - English](https://helpx.adobe.com/content/help/be/en/captivate-prime/get-started.html)
*   [Česká republika](https://helpx.adobe.com/content/help/cz/cs/captivate-prime/get-started.html)
*   [Cyprus - English](https://helpx.adobe.com/content/help/cy/en/captivate-prime/get-started.html)
*   [Danmark](https://helpx.adobe.com/content/help/dk/da/captivate-prime/get-started.html)
*   [Deutschland](https://helpx.adobe.com/content/help/de/de/captivate-prime/get-started.html)
*   [Eesti](https://helpx.adobe.com/content/help/ee/et/captivate-prime/get-started.html)
*   [España](https://helpx.adobe.com/content/help/es/es/captivate-prime/get-started.html)
*   [France](https://helpx.adobe.com/content/help/fr/fr/captivate-prime/get-started.html)
*   [Greece - English](https://helpx.adobe.com/content/help/gr/en/captivate-prime/get-started.html)
*   [Ireland](https://helpx.adobe.com/content/help/ie/en/captivate-prime/get-started.html)
*   [Israel - English](https://helpx.adobe.com/content/help/il/en/captivate-prime/get-started.html)
*   [Italia](https://helpx.adobe.com/content/help/it/it/captivate-prime/get-started.html)
*   [Latvija](https://helpx.adobe.com/content/help/lv/lv/captivate-prime/get-started.html)
*   [Lietuva](https://helpx.adobe.com/content/help/lt/lt/captivate-prime/get-started.html)
*   [Luxembourg - Deutsch](https://helpx.adobe.com/content/help/lu/de/captivate-prime/get-started.html)
*   [Luxembourg - English](https://helpx.adobe.com/content/help/lu/en/captivate-prime/get-started.html)
*   [Luxembourg - Français](https://helpx.adobe.com/content/help/lu/fr/captivate-prime/get-started.html)
*   [Magyarország](https://helpx.adobe.com/content/help/hu/hu/captivate-prime/get-started.html)
*   [Malta - English](https://helpx.adobe.com/content/help/mt/en/captivate-prime/get-started.html)
*   [Middle East and North Africa - English](https://helpx.adobe.com/content/help/mena/en/captivate-prime/get-started.html)
*   [Nederland](https://helpx.adobe.com/content/help/nl/nl/captivate-prime/get-started.html)
*   [Norge](https://helpx.adobe.com/content/help/no/no/captivate-prime/get-started.html)
*   [Österreich](https://helpx.adobe.com/content/help/at/de/captivate-prime/get-started.html)
*   [Polska](https://helpx.adobe.com/content/help/pl/pl/captivate-prime/get-started.html)
*   [Portugal](https://helpx.adobe.com/content/help/pt/pt/captivate-prime/get-started.html)
*   [România](https://helpx.adobe.com/content/help/ro/ro/captivate-prime/get-started.html)
*   [Schweiz](https://helpx.adobe.com/content/help/ch/de/captivate-prime/get-started.html)
*   [Slovenija](https://helpx.adobe.com/content/help/si/sl/captivate-prime/get-started.html)
*   [Slovensko](https://helpx.adobe.com/content/help/sk/sk/captivate-prime/get-started.html)
*   [Suisse](https://helpx.adobe.com/content/help/ch/fr/captivate-prime/get-started.html)
*   [Suomi](https://helpx.adobe.com/content/help/fi/fi/captivate-prime/get-started.html)
*   [Svizzera](https://helpx.adobe.com/content/help/ch/it/captivate-prime/get-started.html)
*   [Türkiye](https://helpx.adobe.com/content/help/tr/tr/captivate-prime/get-started.html)
*   [United Kingdom](https://helpx.adobe.com/content/help/uk/en/captivate-prime/get-started.html)
*   [България](https://helpx.adobe.com/content/help/bg/bg/captivate-prime/get-started.html)
*   [Россия](https://helpx.adobe.com/content/help/ru/ru/captivate-prime/get-started.html)
*   [Україна](https://helpx.adobe.com/content/help/ua/uk/captivate-prime/get-started.html)
*   [الشرق الأوسط وشمال أفريقيا - اللغة العربية](https://helpx.adobe.com/content/help/mena/ar/captivate-prime/get-started.html)
*   [ישראל - עברית](https://helpx.adobe.com/content/help/il/he/captivate-prime/get-started.html)
*   [Sverige](https://helpx.adobe.com/content/help/se/sv/captivate-prime/get-started.html)
*   [Saudi Arabia - English](https://helpx.adobe.com/content/help/sa/en/captivate-prime/get-started.html)
*   [United Arab Emirates - English](https://helpx.adobe.com/content/help/ae/en/captivate-prime/get-started.html)
*   [الإمارات العربية المتحدة](https://helpx.adobe.com/content/help/ae/ar/captivate-prime/get-started.html)
*   [المملكة العربية السعودية](https://helpx.adobe.com/content/help/sa/ar/captivate-prime/get-started.html)

window.dexter = window.dexter || {}; window.dexter.Analytics = window.dexter.Analytics || {}; window.dexter.Analytics.language = 'en\_us'; window.dexter.Analytics.geoRegion = 'US'; window.dexter.Analytics.targetEnabled = '' !== 'disabled'; window.dexter.Analytics.audienceManagerEnabled = '' !== 'disabled'; window.dexter.Analytics.environment = 'production'; window.marketingtech = window.marketingtech || {}; window.marketingtech.adobe = { target: window.dexter.Analytics.targetEnabled, audienceManager: window.dexter.Analytics.audienceManagerEnabled, launch: { property: 'global', environment: window.dexter.Analytics.environment, controlPageLoad: false }, analytics: { additionalAccounts: '' }, targetControlDxf: false };

###### Language Navigation

Language Navigation

[](#)

Choose a region

Selecting a region changes the language and/or content on Adobe.com.

*   Americas
    
*   [Brasil](https://helpx.adobe.com/content/help/br/pt/captivate-prime/get-started.html)
*   [Canada - English](https://helpx.adobe.com/content/help/ca/en/captivate-prime/get-started.html)
*   [Canada - Français](https://helpx.adobe.com/content/help/ca/fr/captivate-prime/get-started.html)
*   [Latinoamérica](https://helpx.adobe.com/content/help/la/es/captivate-prime/get-started.html)
*   [México](https://helpx.adobe.com/content/help/mx/es/captivate-prime/get-started.html)
*   [Chile](https://helpx.adobe.com/content/help/cl/es/captivate-prime/get-started.html)
*   [United States](https://helpx.adobe.com/content/help/en/captivate-prime/get-started.html)
*   Asia Pacific
    
*   [Australia](https://helpx.adobe.com/content/help/au/en/captivate-prime/get-started.html)
*   [Hong Kong S.A.R. of China](https://helpx.adobe.com/content/help/hk/en/captivate-prime/get-started.html)
*   [India - English](https://helpx.adobe.com/content/help/in/en/captivate-prime/get-started.html)
*   [New Zealand](https://helpx.adobe.com/content/help/nz/en/captivate-prime/get-started.html)
*   [Southeast Asia (Includes Indonesia, Malaysia, Philippines, Singapore, Thailand, and Vietnam) - English](https://helpx.adobe.com/content/help/sea/en/captivate-prime/get-started.html)
*   [中国](https://helpx.adobe.com/content/help/cn/zh-Hans/captivate-prime/get-started.html)
*   [中國香港特別行政區](https://helpx.adobe.com/content/help/hk/zh-Hant/captivate-prime/get-started.html)
*   [台灣地區](https://helpx.adobe.com/content/help/tw/zh-Hant/captivate-prime/get-started.html)
*   [日本](https://helpx.adobe.com/content/help/jp/ja/captivate-prime/get-started.html)
*   [한국](https://helpx.adobe.com/content/help/kr/ko/captivate-prime/get-started.html)
*   [Singapore](https://helpx.adobe.com/content/help/sg/en/captivate-prime/get-started.html)
*   [Thailand - English](https://helpx.adobe.com/content/help/th/en/captivate-prime/get-started.html)
*   [ประเทศไทย](https://helpx.adobe.com/content/help/th/th/captivate-prime/get-started.html)
*   Europe, Middle East and Africa
    
*   [Africa - English](https://helpx.adobe.com/content/help/africa/en/captivate-prime/get-started.html)
*   [België - Nederlands](https://helpx.adobe.com/content/help/be/nl/captivate-prime/get-started.html)
*   [Belgique - Français](https://helpx.adobe.com/content/help/be/fr/captivate-prime/get-started.html)
*   [Belgium - English](https://helpx.adobe.com/content/help/be/en/captivate-prime/get-started.html)
*   [Česká republika](https://helpx.adobe.com/content/help/cz/cs/captivate-prime/get-started.html)
*   [Cyprus - English](https://helpx.adobe.com/content/help/cy/en/captivate-prime/get-started.html)
*   [Danmark](https://helpx.adobe.com/content/help/dk/da/captivate-prime/get-started.html)
*   [Deutschland](https://helpx.adobe.com/content/help/de/de/captivate-prime/get-started.html)
*   [Eesti](https://helpx.adobe.com/content/help/ee/et/captivate-prime/get-started.html)
*   [España](https://helpx.adobe.com/content/help/es/es/captivate-prime/get-started.html)
*   [France](https://helpx.adobe.com/content/help/fr/fr/captivate-prime/get-started.html)
*   [Greece - English](https://helpx.adobe.com/content/help/gr/en/captivate-prime/get-started.html)
*   [Ireland](https://helpx.adobe.com/content/help/ie/en/captivate-prime/get-started.html)
*   [Israel - English](https://helpx.adobe.com/content/help/il/en/captivate-prime/get-started.html)
*   [Italia](https://helpx.adobe.com/content/help/it/it/captivate-prime/get-started.html)
*   [Latvija](https://helpx.adobe.com/content/help/lv/lv/captivate-prime/get-started.html)
*   [Lietuva](https://helpx.adobe.com/content/help/lt/lt/captivate-prime/get-started.html)
*   [Luxembourg - Deutsch](https://helpx.adobe.com/content/help/lu/de/captivate-prime/get-started.html)
*   [Luxembourg - English](https://helpx.adobe.com/content/help/lu/en/captivate-prime/get-started.html)
*   [Luxembourg - Français](https://helpx.adobe.com/content/help/lu/fr/captivate-prime/get-started.html)
*   [Magyarország](https://helpx.adobe.com/content/help/hu/hu/captivate-prime/get-started.html)
*   [Malta - English](https://helpx.adobe.com/content/help/mt/en/captivate-prime/get-started.html)
*   [Middle East and North Africa - English](https://helpx.adobe.com/content/help/mena/en/captivate-prime/get-started.html)
*   [Nederland](https://helpx.adobe.com/content/help/nl/nl/captivate-prime/get-started.html)
*   [Norge](https://helpx.adobe.com/content/help/no/no/captivate-prime/get-started.html)
*   [Österreich](https://helpx.adobe.com/content/help/at/de/captivate-prime/get-started.html)
*   [Polska](https://helpx.adobe.com/content/help/pl/pl/captivate-prime/get-started.html)
*   [Portugal](https://helpx.adobe.com/content/help/pt/pt/captivate-prime/get-started.html)
*   [România](https://helpx.adobe.com/content/help/ro/ro/captivate-prime/get-started.html)
*   [Schweiz](https://helpx.adobe.com/content/help/ch/de/captivate-prime/get-started.html)
*   [Slovenija](https://helpx.adobe.com/content/help/si/sl/captivate-prime/get-started.html)
*   [Slovensko](https://helpx.adobe.com/content/help/sk/sk/captivate-prime/get-started.html)
*   [Suisse](https://helpx.adobe.com/content/help/ch/fr/captivate-prime/get-started.html)
*   [Suomi](https://helpx.adobe.com/content/help/fi/fi/captivate-prime/get-started.html)
*   [Svizzera](https://helpx.adobe.com/content/help/ch/it/captivate-prime/get-started.html)
*   [Türkiye](https://helpx.adobe.com/content/help/tr/tr/captivate-prime/get-started.html)
*   [United Kingdom](https://helpx.adobe.com/content/help/uk/en/captivate-prime/get-started.html)
*   [България](https://helpx.adobe.com/content/help/bg/bg/captivate-prime/get-started.html)
*   [Россия](https://helpx.adobe.com/content/help/ru/ru/captivate-prime/get-started.html)
*   [Україна](https://helpx.adobe.com/content/help/ua/uk/captivate-prime/get-started.html)
*   [الشرق الأوسط وشمال أفريقيا - اللغة العربية](https://helpx.adobe.com/content/help/mena/ar/captivate-prime/get-started.html)
*   [ישראל - עברית](https://helpx.adobe.com/content/help/il/he/captivate-prime/get-started.html)
*   [Sverige](https://helpx.adobe.com/content/help/se/sv/captivate-prime/get-started.html)
*   [Saudi Arabia - English](https://helpx.adobe.com/content/help/sa/en/captivate-prime/get-started.html)
*   [United Arab Emirates - English](https://helpx.adobe.com/content/help/ae/en/captivate-prime/get-started.html)
*   [الإمارات العربية المتحدة](https://helpx.adobe.com/content/help/ae/ar/captivate-prime/get-started.html)
*   [المملكة العربية السعودية](https://helpx.adobe.com/content/help/sa/ar/captivate-prime/get-started.html)

var adobeid = { env: '//ims-na1.adobelogin.com', environment: 'prod', jumpToken: { api: '/ims/jumptoken/v1', }, client\_id: 'AdobeSupport1', scope: 'AdobeID,openid,gnav,creative\_cloud,read\_organizations,additional\_info.projectedProductContext,additional\_info.roles', uses\_redirect\_mode: true, locale: 'en\_US', uses\_modal\_mode: false, autoValidateToken: true, api\_parameters: { authorize: { state: { ac: 'AdobeSupport1', } } }, redirect\_uri: window.location.url, onReady: function () { window.dispatchEvent(new Event('dexter:IMSReady')); } }; var privateBeta = ""; var privateFeaturePack = ""; var admittedDomains = ""; // new line document.addEventListener("DOMContentLoaded", function () { if (privateBeta || (privateFeaturePack)) { page.showPrivateBetaOrFeaturePack(privateBeta, privateFeaturePack, admittedDomains); } }); window.dexter.checkout = window.dexter.checkout || {}; window.dexter.checkout.ims = { clientId: { ucv2: 'unified\_checkout\_client', ucv3: 'unified\_checkout\_client\_v3' }, targetScope: { ucv2: 'AdobeID,openid,sao.stock,additional\_info.roles,additional\_info.vat\_id,additional\_info.dob,update\_profile.countryCode', ucv3: 'AdobeID,openid,sao.stock,additional\_info.roles,additional\_info.vat\_id,additional\_info.dob,update\_profile.countryCode, additional\_info.authenticatingAccount' }, timeout: '' }
