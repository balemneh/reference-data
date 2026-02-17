import { Component, OnInit } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { CommonModule } from '@angular/common';
import { MarkdownComponent } from 'ngx-markdown';

@Component({
  selector: 'app-user-guide',
  templateUrl: './user-guide.html',
  styleUrls: ['./user-guide.scss'],
  standalone: true,
  imports: [CommonModule, MarkdownComponent],
})
export class UserGuideComponent implements OnInit {
  docPath = '';

  constructor(private oauthService: OAuthService) {}

  ngOnInit(): void {
    const claims = this.oauthService.getIdentityClaims() as { [key: string]: any };
    console.log('User Guide: Claims:', claims);

    let userRole = 'consumer'; // Default to consumer

    if (claims) {
      const roleMap: { [key: string]: string } = {
        'testuser': 'admin',
        'dsteward': 'data-steward',
        'consumer': 'consumer'
      };
      userRole = roleMap[claims['preferred_username'] as string] || 'consumer';
    }
    
    console.log('User Guide: Detected Role:', userRole);

    if (userRole === 'admin') {
      this.docPath = '/assets/docs/administrator.md';
    } else if (userRole === 'data-steward') {
      this.docPath = '/assets/docs/data-steward.md';
    } else {
      this.docPath = '/assets/docs/consumer.md';
    }

    console.log('User Guide: Selected docPath:', this.docPath);
  }
}