'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Product } from '@/lib/types';
import { productApi } from '@/lib/api';
import { useCart } from '@/context/CartContext';

const CATEGORY_EMOJI: Record<string, string> = {
  Mats: '🧘', Equipment: '💪', Apparel: '👕', Accessories: '🎒', Supplements: '🌿',
};

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { addItem } = useCart();
  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);

  useEffect(() => {
    productApi.get(Number(id))
      .then(data => setProduct(data as Product))
      .catch(() => router.push('/products'))
      .finally(() => setLoading(false));
  }, [id, router]);

  function handleAddToCart() {
    if (!product) return;
    addItem(product, quantity);
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  }

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto px-4 py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
          <div className="bg-gray-100 rounded-2xl h-96 animate-pulse" />
          <div className="space-y-4">
            <div className="h-8 bg-gray-100 rounded-lg animate-pulse" />
            <div className="h-4 bg-gray-100 rounded animate-pulse w-1/2" />
            <div className="h-20 bg-gray-100 rounded-lg animate-pulse" />
          </div>
        </div>
      </div>
    );
  }

  if (!product) return null;

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-6 flex items-center gap-2">
        <Link href="/" className="hover:text-emerald-700">Home</Link>
        <span>/</span>
        <Link href="/products" className="hover:text-emerald-700">Shop</Link>
        <span>/</span>
        <span className="text-gray-900 font-medium truncate">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-10">
        {/* Image */}
        <div className="bg-gradient-to-br from-emerald-50 to-teal-100 rounded-2xl h-80 md:h-full min-h-64 flex items-center justify-center text-9xl">
          {CATEGORY_EMOJI[product.category] ?? '🛍️'}
        </div>

        {/* Details */}
        <div className="flex flex-col gap-4">
          <span className="inline-block text-xs bg-emerald-50 text-emerald-700 font-medium px-3 py-1 rounded-full w-fit">
            {product.category}
          </span>
          <h1 className="text-3xl font-bold text-gray-900 leading-tight">{product.name}</h1>
          <p className="text-3xl font-bold text-emerald-700">${Number(product.price).toFixed(2)}</p>

          <p className="text-gray-600 leading-relaxed">{product.description}</p>

          <div className="flex items-center gap-2 text-sm">
            {product.stock > 0 ? (
              <span className="text-emerald-600 font-medium">
                ✓ In Stock ({product.stock} available)
              </span>
            ) : (
              <span className="text-red-500 font-medium">✗ Out of Stock</span>
            )}
          </div>

          {product.stock > 0 && (
            <div className="flex items-center gap-3">
              <label className="text-sm font-medium text-gray-700">Qty:</label>
              <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden">
                <button
                  onClick={() => setQuantity(q => Math.max(1, q - 1))}
                  className="px-3 py-2 text-gray-600 hover:bg-gray-50 transition-colors font-medium"
                >−</button>
                <span className="px-4 py-2 text-gray-900 font-medium min-w-8 text-center">{quantity}</span>
                <button
                  onClick={() => setQuantity(q => Math.min(product.stock, q + 1))}
                  className="px-3 py-2 text-gray-600 hover:bg-gray-50 transition-colors font-medium"
                >+</button>
              </div>
            </div>
          )}

          <div className="flex gap-3 mt-2">
            <button
              onClick={handleAddToCart}
              disabled={product.stock === 0}
              className={`flex-1 font-bold py-3 rounded-xl transition-colors ${
                added
                  ? 'bg-green-500 text-white'
                  : product.stock === 0
                  ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                  : 'bg-emerald-600 hover:bg-emerald-700 text-white'
              }`}
            >
              {added ? '✓ Added to Cart!' : product.stock === 0 ? 'Out of Stock' : 'Add to Cart'}
            </button>
            <Link href="/cart"
              className="px-6 py-3 border-2 border-emerald-600 text-emerald-700 font-bold rounded-xl hover:bg-emerald-50 transition-colors">
              View Cart
            </Link>
          </div>

          <div className="border-t border-gray-100 pt-4 mt-2 text-sm text-gray-500 space-y-1">
            <p>SKU: <span className="text-gray-700 font-medium">{product.sku}</span></p>
          </div>
        </div>
      </div>
    </div>
  );
}
