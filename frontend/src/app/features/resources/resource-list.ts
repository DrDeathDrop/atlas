import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { EnumLabelPipe } from '../../shared/enum-label.pipe';
import { Resource } from './resource.models';
import { ResourceService } from './resource.service';

@Component({
  selector: 'app-resource-list',
  imports: [EnumLabelPipe, MatButtonModule, MatProgressBarModule, MatTableModule, RouterLink],
  templateUrl: './resource-list.html',
  styleUrl: './resource-list.scss',
})
export class ResourceList implements OnInit {
  private readonly resourceService = inject(ResourceService);
  private readonly auth = inject(AuthService);

  protected readonly mayAdd = computed(() => this.auth.hasRole('ADMIN', 'DISPATCHER'));

  protected readonly columns = ['callSign', 'type', 'kind', 'status', 'location'];
  protected readonly resources = signal<Resource[]>([]);
  protected readonly loading = signal(true);
  protected readonly failed = signal(false);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.failed.set(false);

    this.resourceService.list().subscribe({
      next: (resources) => {
        this.resources.set(resources);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      },
    });
  }
}
