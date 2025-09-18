import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BannerComponent } from './components/banner/banner';
import { ToastContainerComponent } from './components/toast-container/toast-container';
import { CommandPaletteComponent } from './components/command-palette/command-palette';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, BannerComponent, ToastContainerComponent, CommandPaletteComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  title = 'CBP Reference Data Admin';
}