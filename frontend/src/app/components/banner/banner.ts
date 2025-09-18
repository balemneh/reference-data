import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="usa-banner" aria-label="Official government website">
      <div class="usa-accordion">
        <header class="usa-banner__header">
          <div class="usa-banner__inner">
            <div class="grid-col-auto">
              <img
                class="usa-banner__header-flag"
                src="/assets/uswds/img/us_flag_small.png"
                alt="U.S. flag"
              />
            </div>
            <div class="grid-col-fill tablet:grid-col-auto">
              <p class="usa-banner__header-text">
                An official website of the United States government
              </p>
              <button
                class="usa-banner__header-action"
                [attr.aria-expanded]="isExpanded"
                [attr.aria-controls]="'gov-banner'"
                (click)="toggleBanner()"
              >
                Here's how you know
              </button>
            </div>
          </div>
        </header>
        <div
          class="usa-banner__content"
          id="gov-banner"
          [attr.aria-hidden]="!isExpanded"
        >
          <div class="grid-row grid-gap-lg">
            <div class="usa-banner__guidance tablet:grid-col-6">
              <img
                class="usa-banner__icon usa-media-block__img"
                src="/assets/uswds/img/icon-dot-gov.svg"
                alt="Dot gov"
              />
              <div class="usa-media-block__body">
                <p>
                  <strong>Official websites use .gov</strong><br />
                  A <strong>.gov</strong> website belongs to an official
                  government organization in the United States.
                </p>
              </div>
            </div>
            <div class="usa-banner__guidance tablet:grid-col-6">
              <img
                class="usa-banner__icon usa-media-block__img"
                src="/assets/uswds/img/icon-https.svg"
                alt="Https"
              />
              <div class="usa-media-block__body">
                <p>
                  <strong>Secure .gov websites use HTTPS</strong><br />
                  A <strong>lock</strong> (
                  <span class="icon-lock">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      width="52"
                      height="64"
                      viewBox="0 0 52 64"
                      class="usa-banner__lock-image"
                      role="img"
                      aria-labelledby="banner-lock-title banner-lock-description"
                    >
                      <title id="banner-lock-title">Lock</title>
                      <desc id="banner-lock-description">A locked padlock</desc>
                      <path
                        fill="#000000"
                        fill-rule="evenodd"
                        d="M26 0c10.493 0 19 8.507 19 19v9h3a4 4 0 0 1 4 4v28a4 4 0 0 1-4 4H4a4 4 0 0 1-4-4V32a4 4 0 0 1 4-4h3v-9C7 8.507 15.507 0 26 0zm0 8c-5.979 0-10.843 4.77-10.996 10.712L15 19v9h22v-9c0-6.075-4.925-11-11-11z"
                      />
                    </svg>
                  </span>
                  ) or <strong>https://</strong> means you've safely connected to
                  the .gov website. Share sensitive information only on official,
                  secure websites.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .usa-banner {
      background-color: #f0f0f0;
      font-family: Source Sans Pro Web, Helvetica Neue, Helvetica, Roboto, Arial, sans-serif;
      position: relative;
      z-index: 1010;
    }

    .usa-accordion {
      width: 100%;
    }

    .usa-banner__header {
      padding: 0.25rem 0;
      font-size: 0.9rem;
      font-weight: 400;
      background-color: #f0f0f0;
    }

    .usa-banner__inner {
      display: flex;
      align-items: center;
      padding: 0 1rem;
      max-width: 100%;
      margin: 0;
    }

    .usa-banner__header-flag {
      width: 1rem;
      height: auto;
      margin-right: 0.5rem;
    }

    .grid-col-auto {
      flex: 0 0 auto;
    }

    .grid-col-fill {
      flex: 1 1 auto;
    }

    .usa-banner__header-text {
      margin: 0;
      font-size: 0.75rem;
      line-height: 1.1;
      display: inline;
    }

    .usa-banner__header-action {
      background-color: transparent;
      border: 0;
      border-radius: 0;
      color: #005ea2;
      cursor: pointer;
      display: inline;
      font-weight: 400;
      margin: 0 0 0 0.5rem;
      padding: 0;
      text-decoration: underline;
      font-size: 0.75rem;
      text-align: left;
    }

    .usa-banner__header-action:hover {
      color: #1a4480;
    }

    .usa-banner__header-action::after {
      content: "";
      display: inline-block;
      width: 0;
      height: 0;
      margin-left: 0.25rem;
      border-left: 3px solid transparent;
      border-right: 3px solid transparent;
      border-top: 3px solid #005ea2;
      vertical-align: middle;
    }

    .usa-banner__header-action[aria-expanded="true"]::after {
      border-top: 0;
      border-bottom: 3px solid #005ea2;
    }

    .usa-banner__content {
      padding: 1rem;
      background-color: #f0f0f0;
      border-top: 1px solid #dfe1e2;
      display: none;
    }

    .usa-banner__content[aria-hidden="false"] {
      display: block;
    }

    .grid-row {
      display: flex;
      flex-wrap: wrap;
      gap: 2rem;
    }

    .usa-banner__guidance {
      display: flex;
      align-items: flex-start;
      flex: 1;
      min-width: 250px;
    }

    .usa-banner__icon {
      width: 2rem;
      height: 2rem;
      margin-right: 0.75rem;
      flex-shrink: 0;
    }

    .usa-media-block__body {
      flex: 1;
    }

    .usa-media-block__body p {
      margin: 0;
      font-size: 0.875rem;
      line-height: 1.4;
    }

    .usa-banner__lock-image {
      width: 0.75rem;
      height: auto;
      vertical-align: middle;
    }

    .icon-lock {
      display: inline-block;
      vertical-align: middle;
    }

    @media (max-width: 640px) {
      .grid-row {
        flex-direction: column;
      }
      
      .usa-banner__guidance {
        margin-bottom: 1rem;
      }
    }
  `]
})
export class BannerComponent {
  isExpanded = false;

  toggleBanner() {
    this.isExpanded = !this.isExpanded;
  }
}
