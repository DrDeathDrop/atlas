import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { LiveUpdates } from '../core/live/live-updates';

@Component({
  selector: 'app-shell',
  imports: [MatButtonModule, MatToolbarModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly live = inject(LiveUpdates);

  protected readonly user = this.auth.user;
  protected readonly isLive = this.live.connected;

  ngOnInit(): void {
    this.live.start();
  }

  ngOnDestroy(): void {
    this.live.stop();
  }

  protected logout(): void {
    this.auth.logout().subscribe(() => void this.router.navigate(['/login']));
  }
}
