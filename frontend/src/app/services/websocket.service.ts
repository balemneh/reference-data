import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject, timer } from 'rxjs';
import { filter, map, retry, share } from 'rxjs/operators';

export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp?: Date;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private socket?: WebSocket;
  private messagesSubject = new Subject<WebSocketMessage>();
  private connectionStatus = new BehaviorSubject<boolean>(false);
  private reconnectTimer?: any;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private reconnectDelay = 1000; // Start with 1 second

  public messages$ = this.messagesSubject.asObservable().pipe(share());
  public connectionStatus$ = this.connectionStatus.asObservable();

  constructor() {
    this.connect();
  }

  private connect(): void {
    // Determine WebSocket URL based on current location
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = '8081'; // API port
    const wsUrl = `${protocol}//${host}:${port}/ws`;

    try {
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        console.log('WebSocket connected');
        this.connectionStatus.next(true);
        this.reconnectAttempts = 0;
        this.reconnectDelay = 1000;
        
        // Subscribe to all data updates
        this.send({
          type: 'subscribe',
          data: {
            topics: ['countries', 'ports', 'airports', 'change-requests', 'system-status']
          }
        });
      };

      this.socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          this.messagesSubject.next({
            ...message,
            timestamp: new Date()
          });
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };

      this.socket.onclose = () => {
        console.log('WebSocket disconnected');
        this.connectionStatus.next(false);
        this.socket = undefined;
        this.scheduleReconnect();
      };
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    this.reconnectAttempts++;
    console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${this.reconnectDelay}ms`);

    this.reconnectTimer = setTimeout(() => {
      this.connect();
    }, this.reconnectDelay);

    // Exponential backoff
    this.reconnectDelay = Math.min(this.reconnectDelay * 2, 30000); // Max 30 seconds
  }

  public send(message: any): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    } else {
      console.warn('WebSocket is not connected');
    }
  }

  public on<T>(messageType: string): Observable<T> {
    return this.messages$.pipe(
      filter(message => message.type === messageType),
      map(message => message.data as T)
    );
  }

  public subscribe(topic: string): void {
    this.send({
      type: 'subscribe',
      data: { topic }
    });
  }

  public unsubscribe(topic: string): void {
    this.send({
      type: 'unsubscribe',
      data: { topic }
    });
  }

  ngOnDestroy(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }
    if (this.socket) {
      this.socket.close();
    }
    this.messagesSubject.complete();
    this.connectionStatus.complete();
  }
}