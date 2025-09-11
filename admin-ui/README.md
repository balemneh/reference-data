# CBP Reference Data Admin UI

Angular 20 administrative interface for the CBP Reference Data Service, featuring USWDS 3.13 design system and CBP branding.

## Features

- **Government-compliant UI** with USWDS 3.13 (US Web Design System)
- **CBP branding** with official logo and color scheme
- **Consolidated navigation** - no duplicate menu items
- **Responsive design** for desktop and mobile
- **Accessibility** - WCAG AA compliant
- **Modern Angular 20** with TypeScript 5.8

## UI Components

### Navigation Structure
- **Header**: Streamlined with user menu, notifications, and minimal help dropdown
- **Sidebar**: Complete navigation hub with:
  - Main navigation (Dashboard)
  - Quick actions (Add/Import/Export Data)
  - Reference Data section
  - Operations (Change Requests)
  - Settings & Administration
  - Help & Support section

### Visual Improvements (Implemented)
- ✅ Increased spacing between banner, header, and breadcrumbs
- ✅ Consistent sidebar typography with strong active state highlighting
- ✅ Balanced KPI card visual weight
- ✅ High-contrast status labels (ACTIVE, PENDING, EXCELLENT)
- ✅ WCAG AA compliant color scheme
- ✅ Loading indicators for async operations
- ✅ Smart breadcrumb hiding when only one level

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
