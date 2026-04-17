"use client";

import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";
import createGlobe, { COBEOptions } from "cobe"
import { useCallback, useEffect, useRef, useState } from "react"
import { cn } from "@/lib/utils"
import Link from "next/link";

export function GlobeFeatureSection({ stats }: { stats: any }) {
  return (
    <section className="relative w-full mx-auto overflow-hidden rounded-3xl bg-muted/30 border border-border shadow-soft px-6 py-16 md:px-16 md:py-24 mt-32 z-10">
      <div className="absolute inset-0 bg-gradient-to-r from-accent/5 to-accent-light/5 pointer-events-none" />
      <div className="flex flex-col-reverse items-center justify-between gap-10 md:flex-row relative z-10">
        <div className="z-10 max-w-xl text-left">
          <h2 className="text-3xl lg:text-4xl font-extrabold text-text-primary mb-4 tracking-tight">
            Built with <span className="text-accent">Global Precision</span>
          </h2>
          <p className="text-lg text-text-secondary font-medium mb-8">
            Empower your infrastructure with the AppointUnified network. 
            Currently serving <span className="text-accent-mint font-bold">{stats ? stats.totalProfessionals.toLocaleString() : "..."} nodes</span> routing
            schedules perfectly with pinpoint accuracy. Join the engine.
          </p>
          <div className="flex items-center gap-4">
            <Link href="/auth/signup">
              <Button className="inline-flex items-center gap-2 rounded-full px-6 py-6 shadow-medium hover:scale-105 transition-transform bg-accent hover:bg-accent-glow text-white">
                Join Network <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </div>
        <div className="relative min-h-[300px] md:min-h-[400px] w-full max-w-xl flex items-center justify-center md:justify-end overflow-visible">
          <div className="w-full h-full max-w-[400px] md:max-w-[500px]">
             <Globe className="relative w-full h-full aspect-square" stats={stats} />
          </div>
        </div>
      </div>
    </section>
  );
}

const GLOBE_CONFIG: Omit<COBEOptions, "onRender"> = {
  width: 800,
  height: 800,
  devicePixelRatio: 2,
  phi: 0,
  theta: 0.3,
  dark: 0, // We are in a light theme Pastel setup, 0 works. If we want dark mode we use 1.
  diffuse: 0.4,
  mapSamples: 16000,
  mapBrightness: 1.2,
  baseColor: [1, 1, 1], // Pure white base
  markerColor: [129 / 255, 140 / 255, 248 / 255], // Pastel Purple Accent `bg-accent` roughly #818CF8
  glowColor: [240 / 255, 243 / 255, 255 / 255], 
  markers: [
    // We will dynamically override this if backend pins available.
    { location: [14.5995, 120.9842], size: 0.03 },
    { location: [19.076, 72.8777], size: 0.1 },
    { location: [23.8103, 90.4125], size: 0.05 },
    { location: [30.0444, 31.2357], size: 0.07 },
    { location: [39.9042, 116.4074], size: 0.08 },
    { location: [-23.5505, -46.6333], size: 0.1 },
    { location: [19.4326, -99.1332], size: 0.1 },
    { location: [40.7128, -74.006], size: 0.1 },
    { location: [34.6937, 135.5022], size: 0.05 },
    { location: [41.0082, 28.9784], size: 0.06 },
  ],
}

export function Globe({
  className,
  config = GLOBE_CONFIG,
  stats
}: {
  className?: string
  config?: Omit<COBEOptions, "onRender">
  stats?: any
}) {
  let phi = 0
  let width = 0
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const pointerInteracting = useRef(null)
  const pointerInteractionMovement = useRef(0)
  const [r, setR] = useState(0)
  const [mergedConfig, setMergedConfig] = useState<Omit<COBEOptions, "onRender">>(config)

  useEffect(() => {
    if (stats && stats.mapPins && stats.mapPins.length > 0) {
      const scaledPins = stats.mapPins.map((pin: any) => ({
        location: [pin.latitude, pin.longitude],
        size: 0.06
      }))
      setMergedConfig(prev => ({ ...prev, markers: scaledPins }))
    }
  }, [stats])

  const updatePointerInteraction = (value: any) => {
    pointerInteracting.current = value
    if (canvasRef.current) {
      canvasRef.current.style.cursor = value ? "grabbing" : "grab"
    }
  }

  const updateMovement = (clientX: any) => {
    if (pointerInteracting.current !== null) {
      const delta = clientX - pointerInteracting.current
      pointerInteractionMovement.current = delta
      setR(delta / 200)
    }
  }

  const onRender = useCallback(
    (state: Record<string, any>) => {
      if (!pointerInteracting.current) phi += 0.005
      state.phi = phi + r
      state.width = width * 2
      state.height = width * 2
    },
    [r],
  )

  const onResize = () => {
    if (canvasRef.current) {
      width = canvasRef.current.offsetWidth
    }
  }

  useEffect(() => {
    window.addEventListener("resize", onResize)
    onResize()

    const globe = createGlobe(canvasRef.current!, {
      ...mergedConfig,
      width: width * 2,
      height: width * 2,
      onRender,
    } as any)

    setTimeout(() => {
      if (canvasRef.current) {
        canvasRef.current.style.opacity = "1"
      }
    })
    return () => globe.destroy()
  }, [mergedConfig, onRender])

  return (
    <div
      className={cn(
        "relative mx-auto aspect-square w-full max-w-[600px]",
        className,
      )}
    >
      <canvas
        className={cn(
          "size-full opacity-0 transition-opacity duration-500 [contain:layout_paint_size]",
        )}
        ref={canvasRef}
        onPointerDown={(e) =>
          updatePointerInteraction(
            e.clientX - pointerInteractionMovement.current,
          )
        }
        onPointerUp={() => updatePointerInteraction(null)}
        onPointerOut={() => updatePointerInteraction(null)}
        onMouseMove={(e) => updateMovement(e.clientX)}
        onTouchMove={(e) =>
          e.touches[0] && updateMovement(e.touches[0].clientX)
        }
      />
    </div>
  )
}
